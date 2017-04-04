
import club.minnced.kjda.embed
import club.minnced.kjda.entities.editAsync
import club.minnced.kjda.entities.sendText
import club.minnced.kjda.plusAssign
import club.minnced.kjda.then
import com.futuremangaming.futurebot.Permissions
import gnu.trove.list.array.TLongArrayList
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.security.SecureRandom

object Giveaways { // utility

    private val MAP = TLongObjectHashMap<Giveaway>()

    fun giveFor(channel: TextChannel): Giveaway {
        if (!MAP.containsKey(channel.idLong))
            MAP.put(channel.idLong, Giveaway(channel))
        return MAP[channel.idLong]
    }

    fun canClose(mem: Member)
        = Permissions.isModerator(mem)

}


class Giveaway(val channel: TextChannel) { // actual giveaway

    private val participants = TLongArrayList()
    internal var join: String? = null
    internal var end: String? = null
    internal var sub: Boolean = false
    private val random = SecureRandom()
    private val adapter = GiveawayAdapter(this)

    var message: Long = -1

    fun open(enter: String, close: String, sub: Boolean = false): Message {
        if (join !== null) close()

        join = enter
        end = close
        this.sub = sub

        participants.clear()

        channel.jda.addEventListener(adapter)

        val message = channel.sendText { "Starting Giveaway..." }
        this.message = message.idLong
        message.addReaction(enter).complete()
        message.addReaction(close).complete()
        if (channel.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE))
            message.pin().queue()
        return message
    }

    fun close(entries: Int = entrySize()) {
        if (join === null) return
        join = null

        channel.jda.removeEventListener(adapter)
        channel.getMessageById(message) then {
            this?.editAsync {
                this += "🔒 Giveaway is closed 🔒"
                embed {
                    this += "Type `!draw` to poll the next winner! [Entries: $entries]"
                    color { 0x50aace }
                }
            }

            if (channel.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE))
                this?.unpin()?.queue()
        }
    }

    fun reset(message: Long) {
        participants.clear()

        if (join !== null)
            channel.addReactionById(message, join)
        else
            channel.addReactionById(message, end)
    }

    fun enter(id: Long) {
        participants.add(id)
    }

    fun exit(id: Long) {
        participants.remove(id)
    }

    fun entrySize() = participants.size()

    fun pollWinner(): Long {
        close()

        val rng = random.nextInt(participants.size())
        val winner = participants[rng]
        participants.remove(winner)

        return winner
    }

}

class GiveawayAdapter(val giveaway: Giveaway) : ListenerAdapter() {

    override fun onGenericMessageReaction(event: GenericMessageReactionEvent?) {
        super.onGenericMessageReaction(event)
    }

    override fun onMessageReactionRemoveAll(event: MessageReactionRemoveAllEvent) {
        val id = event.messageId.toLong()
        if (id != giveaway.message) return

        giveaway.reset(id)
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user == event.jda.selfUser) return
        val id = event.user.idLong
        val messageId = event.messageId.toLong()
        val react = event.reaction.emote.id ?: event.reaction.emote.name

        val channel = event.channel as? TextChannel ?: return
        val guild = channel.guild
        val member = guild.getMember(event.user)

        if (messageId != giveaway.message) return
        if (react != giveaway.join) {
            if (react == giveaway.end && Giveaways.canClose(member))
                giveaway.close()
            return
        }

        if (giveaway.sub && !Permissions.isSubscriber(member)) return

        giveaway.enter(id)
    }

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        if (event.user == event.jda.selfUser) return
        val id = event.user.idLong
        val messageId = event.messageId.toLong()

        if (messageId != giveaway.message) return
        if (event.reaction.emote.id ?: event.reaction.emote.name != giveaway.join) return

        giveaway.exit(id)
    }
}
