package me.soknight.easydesk.channel.vkontakte.vk

import me.soknight.easydesk.channel.vkontakte.vk.model.VkUpdate
import kotlin.reflect.KClass

class VkBehaviourContext internal constructor(val bot: VkBot) {

    private val handlers =
        mutableMapOf<KClass<out VkUpdate>, MutableList<suspend VkBehaviourContext.(VkUpdate) -> Unit>>()

    internal fun <T : VkUpdate> register(
        type: KClass<T>,
        handler: suspend VkBehaviourContext.(T) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(type) { mutableListOf() } += handler as suspend VkBehaviourContext.(VkUpdate) -> Unit
    }

    // Handlers for the same type are called sequentially in registration order.
    internal suspend fun dispatch(update: VkUpdate) {
        handlers[update::class]?.forEach { it(update) }
    }

}

fun VkBehaviourContext.onMessageAllow(handler: suspend VkBehaviourContext.(VkUpdate.MessageAllow) -> Unit) =
    register(VkUpdate.MessageAllow::class, handler)

fun VkBehaviourContext.onMessageDeny(handler: suspend VkBehaviourContext.(VkUpdate.MessageDeny) -> Unit) =
    register(VkUpdate.MessageDeny::class, handler)

fun VkBehaviourContext.onMessageEdit(handler: suspend VkBehaviourContext.(VkUpdate.MessageEdit) -> Unit) =
    register(VkUpdate.MessageEdit::class, handler)

fun VkBehaviourContext.onMessageNew(handler: suspend VkBehaviourContext.(VkUpdate.MessageNew) -> Unit) =
    register(VkUpdate.MessageNew::class, handler)

fun VkBehaviourContext.onMessageReaction(handler: suspend VkBehaviourContext.(VkUpdate.MessageReaction) -> Unit) =
    register(VkUpdate.MessageReaction::class, handler)

fun VkBehaviourContext.onMessageRead(handler: suspend VkBehaviourContext.(VkUpdate.MessageRead) -> Unit) =
    register(VkUpdate.MessageRead::class, handler)

fun VkBehaviourContext.onMessageReply(handler: suspend VkBehaviourContext.(VkUpdate.MessageReply) -> Unit) =
    register(VkUpdate.MessageReply::class, handler)

fun VkBehaviourContext.onTypingState(handler: suspend VkBehaviourContext.(VkUpdate.TypingState) -> Unit) =
    register(VkUpdate.TypingState::class, handler)
