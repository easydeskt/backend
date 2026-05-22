package me.soknight.easydesk.channel.vkontakte.util

private enum class NativeReaction(val id: Int, val emoji: String) {

    ANGRY          (id =  8, emoji = "😡"),
    BLOWING_KISS   (id = 14, emoji = "😘"),
    BROKEN_HEART   (id = 23, emoji = "💔"),
    CHECK_MARK     (id = 28, emoji = "✅"),
    CLAPPING       (id = 26, emoji = "👏"),
    CLOWN          (id = 17, emoji = "🤡"),
    CRYING         (id =  7, emoji = "😭"),
    EYES           (id = 32, emoji = "👀"),
    FIRE           (id =  2, emoji = "🔥"),
    FOLDED_HANDS   (id = 13, emoji = "🙏"),
    FULL_MOON_FACE (id = 34, emoji = "🌚"),
    HANDSHAKE      (id = 18, emoji = "🤝"),
    HEART          (id =  1, emoji = "❤️"),
    HEART_EYES     (id = 15, emoji = "😍"),
    HUNDRED        (id = 35, emoji = "💯"),
    IMP            (id = 39, emoji = "😈"),
    JOY            (id =  3, emoji = "😂"),
    LAUGHING       (id = 11, emoji = "😄"),
    LIGHTNING      (id = 64, emoji = "⚡️"),
    MIND_BLOWN     (id = 37, emoji = "🤯"),
    MOAI           (id = 21, emoji = "🗿"),
    NAIL_POLISH    (id = 36, emoji = "💅"),
    NAUSEATED      (id = 42, emoji = "🤮"),
    OK_HAND        (id = 10, emoji = "👌"),
    PARTY_POPPER   (id = 16, emoji = "🎉"),
    PLEADING       (id = 20, emoji = "😐"),
    POOP           (id =  5, emoji = "💩"),
    QUESTION       (id =  6, emoji = "❓"),
    ROLLING_EYES   (id = 22, emoji = "🙄"),
    SAD            (id = 27, emoji = "😥"),
    SALUTING       (id = 40, emoji = "🫡"),
    SCREAMING      (id = 30, emoji = "😱"),
    SLEEPING       (id = 38, emoji = "😴"),
    SUNGLASSES     (id = 24, emoji = "😎"),
    SURPRISED      (id = 19, emoji = "😲"),
    SWEARING       (id = 31, emoji = "🤬"),
    THINKING       (id = 12, emoji = "🤔"),
    THUMBS_DOWN    (id =  9, emoji = "👎"),
    THUMBS_UP      (id =  4, emoji = "👍"),
    TROPHY         (id = 29, emoji = "🏆"),
    ;

    companion object {
        val byId = entries.associateBy(NativeReaction::id, NativeReaction::emoji)
        val byEmoji = entries.associateBy(NativeReaction::emoji, NativeReaction::id)
    }

}

internal fun emojiFromNativeReactionOrNull(reactionId: Int): String? =
    NativeReaction.byId[reactionId]

internal fun nativeReactionFromEmojiOrNull(emoji: String): Int? =
    NativeReaction.byEmoji[emoji]
