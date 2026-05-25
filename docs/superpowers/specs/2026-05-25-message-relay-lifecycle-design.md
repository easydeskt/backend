# Message Relay Lifecycle — Design Spec

**Date:** 2026-05-25
**Scope:** Full bidirectional message lifecycle across all channels (Telegram, Email, VKontakte), including attachments with in-memory buffering, platform-specific rendering, sticker support, and ConversationRegistry resilience.

---

## Goal

Close all gaps in the message forwarding cycle between client channels and the agent supergroup topic. After implementation:

- All supported attachment types flow in both directions across all channels
- Platform-specific rendering is handled natively inside each channel's `Conversation.send()`
- No disk caching of ticket message attachments — metadata only in DB, bytes flow through RAM
- Agent replies survive bot restarts via `ConversationFactory` DB restoration

---

## Out of Scope

- Reply template attachments (`service:storage`) — unchanged
- S3 / remote storage for attachments
- Sticker forwarding for VKontakte (dropped)
- Cross-channel sticker forwarding (TG stickers only relay within Telegram)

---

## Architecture

### Message flows

```
[Client] → [Channel receive] → [EventBus] → [service:tickets] → [EventBus]
       → [TelegramMessageRelayHandler] → [Supergroup topic]

[Agent] → [Supergroup topic] → [TelegramAgentReplyHandler]
       → [ConversationRegistry] → [Conversation.send(message)] → [Client]
```

### Primary dispatch: `Conversation.send(message: Message)`

Each channel implements `send(message)` directly, inspecting `message.plainText` and `message.attachments` to produce the correct platform-specific API calls. The `send(block)` variant remains for convenience (text-only, programmatic construction) and may delegate into `send(message)` via a temporary builder.

---

## Supported Attachment Types

### Telegram (incoming from client)

| Telegram content type | `Attachment.Kind` | Storage |
|-----------------------|-------------------|---------|
| `PhotoContent` | PHOTO | ≤ 20 MB → download + `file_id` in attributes; > 20 MB → `file_id` only |
| `DocumentContent` | DOCUMENT | same |
| `VideoContent` | VIDEO | same |
| `VoiceContent` | VOICE | same |
| `AudioContent` | AUDIO | same |
| `StickerContent` | STICKER | `file_id` in attributes only, **never download** |
| Other | — | ignore |

> **Telegram Bot API limit:** `getFile()` supports up to 20 MB. Files > 20 MB are stored as `file_id`-only; cross-channel forwarding of such files is not supported.

### Email (incoming from client)

Handled by existing `MimeMessageMapper`. Add size check: if `part.size > 50 MB` → skip attachment, log warn.

### VKontakte (incoming from client)

| `VkAttachment` type | `Attachment.Kind` | Byte source |
|---------------------|-------------------|-------------|
| `Photo` | PHOTO | Largest size URL |
| `Document` | DOCUMENT | `url` from DTO |
| `AudioMessage` | VOICE | `linkMp3` from DTO |
| `Audio` | AUDIO | URL if available, else skip |
| `Video` | VIDEO | URL if available, else `attributes["vk.player_url"]` |
| `Sticker` | — | **drop** |
| Other | — | skip |

---

## Attachment Storage Model

### Ticket message attachments (new)

New table `ticket_message_attachments` — metadata only, **no `storage_path`**:

```
id, message_id, kind, file_name, content_type, file_size, channel_brand, attributes
```

`attributes` holds remote references:
- `telegram.file_id` — Telegram file identifier
- `vk.player_url` — VK video player URL (fallback when bytes unavailable)

`AttachmentStorageService` and `AttachmentsTable` remain unchanged (template attachments only).

### File size limit

- Global limit: **50 MB**, configurable via `application.yaml`
- For Telegram: effective download limit is 20 MB (Bot API constraint), stored in config as separate note
- Validation happens before download when size is known upfront; during streaming otherwise

---

## Platform-Specific Rendering

### `TelegramConversation.send(message)`

| Message content | API call |
|-----------------|----------|
| Text only | `sendMessage(text)` |
| 1 photo ± text | `sendPhoto(caption = text)` |
| 1 document ± text | `sendDocument(caption = text)` |
| 1 video ± text | `sendVideo(caption = text)` |
| Voice | `sendVoice()` |
| Audio | `sendAudio()` |
| Sticker (TG client only) | `sendSticker(file_id from attributes)` |
| 2–10 mixed media | `sendMediaGroup(items)`, text as caption of first item |
| > 10 media | split into groups of 10 |
| Sticker → non-TG client | skip silently, log warn |

Media is sent by `file_id` when available (no re-upload). Raw bytes used only when `file_id` is absent.

### `EmailConversation.send(message)`

```
multipart/mixed
  ├── multipart/alternative
  │     ├── text/plain
  │     └── text/html
  └── MimeBodyPart (repeated per attachment)
        Content-Disposition: attachment; filename="..."
        Content-Transfer-Encoding: base64
```

Sticker attachments skipped (no bytes available).

### `VKontakteConversation.send(message)`

Three-step upload per attachment:
1. Get upload URL: `docs.getMessagesUploadServer` / `photos.getMessagesUploadServer`
2. POST file bytes to upload server (multipart)
3. Save: `docs.save` / `photos.saveMessagesPhoto` → attachment string `doc{owner}_{id}` / `photo{owner}_{id}`

Single `sendMessage(text, attachments = [...])` after all uploads complete.
Sticker attachments skipped.

---

## Cross-Channel Relay (In-Memory Buffering)

When forwarding between platforms (e.g., Telegram agent → VK client), bytes are **never written to disk**:

```
Source channel download → kotlinx.io.Buffer (RAM) → destination channel upload
```

This applies to:
- Agent Telegram attachment → VK client (download file_id → upload to VK)
- Agent Telegram attachment → Email client (download file_id → MIME part)
- VK client attachment → Telegram topic (download VK URL → sendDocument to topic)

For Telegram→Telegram: always use `file_id` directly, no download.

---

## Relay: Client Message → Supervisor Topic

`TelegramMessageRelayHandler` is updated to relay attachments after receiving `TicketMessageEvent.Recorded`:

- Reads attachment metadata + `attributes` from `ticket_message_attachments`
- **Telegram source:** sends via `file_id` using appropriate `send*` method
- **Other source:** downloads bytes into buffer, sends to topic via `sendDocument` / `sendPhoto` etc.
- Text prefix `📩 [ClientName]` becomes caption when single-media message; separate message otherwise
- Sticker from TG client: `sendSticker(file_id)`

---

## ConversationRegistry Resilience

### Problem

Registry is in-memory only. After restart, agent replies to relayed messages drop with a warn log.

### Solution: `ConversationFactory`

New interface in `channel:api`:

```kotlin
interface ConversationFactory {
    val brand: ChannelBrand
    suspend fun restore(channel: Channel, nativeId: String, attributes: Attributes): Conversation?
}
```

Each channel provider implements it:

| Channel | `nativeId` source | Restored as |
|---------|-------------------|-------------|
| Telegram | `channel_identities.native_id` (Telegram `chat_id`) | `TelegramConversation(bot, channel, chatId)` |
| Email | Sender email address | `EmailConversation(channel, recipientAddress)` + subject from `attributes` |
| VKontakte | `peer_id` | `VKontakteConversation(bot, channel, peerId)` |

### Updated registry lookup

```kotlin
fun getOrNull(conversationId: Long): Conversation? =
    inMemory[conversationId] ?: restoreFromDb(conversationId)?.also { inMemory[conversationId] = it }
```

`restoreFromDb` queries `channel_identities` by `conversationId`, matches provider by brand, calls `factory.restore()`. No new DB columns required.

---

## New Attachment Subtype in `channel:api`

Add `Sticker` to the `Attachment` sealed interface hierarchy (alongside `Audio`, `Document`, `Photo`, `Video`, `Voice`). No extra properties — `file_id` lives in `attributes["telegram.file_id"]`. `Attachment.Kind.STICKER` already exists in the enum.

`AttachmentEntity.toDomain()` currently maps `Kind.STICKER → Attachment.Document` — update to `Attachment.Sticker`.

---

## Database Migration

New migration in `service:tickets`:

```sql
-- V20260525_120000__tickets__ticket_message_attachments.sql
-- attachment_kind ENUM is owned by service:storage migrations — reuse it here.
CREATE TABLE ticket_message_attachments (
    id             BIGSERIAL PRIMARY KEY,
    message_id     BIGINT          NOT NULL,
    kind           attachment_kind NOT NULL,
    file_name      VARCHAR(512)    NOT NULL,
    content_type   VARCHAR(128)    NOT NULL,
    file_size      BIGINT,
    channel_brand  VARCHAR(64)     NOT NULL,
    attributes     JSONB           NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ     NOT NULL
);
```

> `attachment_kind` PostgreSQL ENUM is already created by the `service:storage` module migration. Do not re-declare it.

---

## Test Coverage

| Component | What is tested |
|-----------|----------------|
| `AttachmentStorageService` | 50 MB limit enforcement, file naming, directory structure |
| `TelegramConversation.send()` | Each attachment kind → correct API call; media group batching |
| `EmailConversation.send()` | MIME structure without and with attachments |
| `VKontakteConversation.send()` | Upload flow, attachment string format |
| `TelegramMessageRelayHandler` | Text relay, file_id relay, buffered relay from non-TG source |
| `TelegramAgentReplyHandler` | Parsing each media type; cross-channel forwarding |
| `ConversationRegistry` | Cache miss → factory restoration |
| VK attachment bridge | `VkAttachment → Attachment` for each supported type |
| `MimeMessageMapper` | Attachments over size limit are skipped |

All tests are unit tests. External dependencies (Telegram Bot API, SMTP, VK API) are mocked.
