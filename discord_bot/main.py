import json
import discord
from discord.ext import tasks
from model import (
    MinecraftChatClient, 
    MinecraftMessage,
    MessageType,
    MentionData,
    parse_markdown,
    convert_tokens_to_json
)
from config import (
    token,
    minecraft_channel_id,
    minecraft_server_ip,
    minecraft_socket_server_port,
    reconnect_time
)

bot = discord.Client(intents=discord.Intents.all())

chat_client: MinecraftChatClient = None

minecraft_channel: discord.TextChannel = None

@bot.event
async def on_ready() -> None:
    global chat_client, minecraft_channel

    chat_client = MinecraftChatClient(
        minecraft_server_ip, 
        minecraft_socket_server_port, 
        bot, 
        on_minecraft_message
    )

    minecraft_channel = bot.get_channel(minecraft_channel_id)

    connect_to_minecraft_chat.start()


@tasks.loop(seconds=reconnect_time)
async def connect_to_minecraft_chat() -> None:
    if not chat_client.connected:
        await chat_client.connect()

@bot.event
async def on_message(message: discord.Message) -> None:
    if (message.channel != minecraft_channel or message.author == bot.user):
        return

    if chat_client.connected:
        for command in prepare_tellraw_commands(message):
            await chat_client.send_request(command)


async def on_minecraft_message(message: MinecraftMessage) -> None:
    if message.type == MessageType.Message:
        return await minecraft_channel.send(f'`<{message.player_name}>` {message.content}')
    
    if message.type in (MessageType.PlayerJoin, MessageType.PlayerLeft):
        color = (discord.Color.green() if message.type == MessageType.PlayerJoin
                else discord.Color.red())

        return await minecraft_channel.send(
            embed=discord.Embed(
                description=f'**{message.player_name}** {message.content}', 
                color=color
            )
        ) 

    if message.type == MessageType.PlayerDie:
        return await minecraft_channel.send(
            embed=discord.Embed(
                description=message.content, 
                color=discord.Color.default()
            )
        ) 

    if message.type == MessageType.AdvancementMade:
        return await minecraft_channel.send(
            embed=discord.Embed(
                description=f'{message.player_name} получил достижение **{message.content}**', 
                color=discord.Color.gold()
            )
        ) 

def prepare_tellraw_commands(message: discord.Message) -> list[str]:
    result = []
    
    user_name = message.author.nick or message.author.name
    role_color = str(message.author.top_role.color)

    base_part = [
        '',
        {'text': '[discord]', 'bold': True, 'color': '#F1C40F'},
        {'text': ' <'},
        {'text': user_name, 'color': role_color},
        {'text': '> '}
    ]

    if message.content:
        mentions = {member.mention: MentionData(member) for member in message.mentions}
        mentions.update({role.mention: MentionData(role) for role in message.role_mentions})
        mentions.update({channel.mention: MentionData(channel) for channel in message.channel_mentions})

        try:
            text_part = convert_tokens_to_json(parse_markdown(message.content), mentions)
        except:
            for mention, object in mentions.items():
                message.content = message.content.replace(mention, f'@{object.name}')
            text_part = [{'text': message.content}]

        result.append('/tellraw @a ' + json.dumps(base_part + text_part, ensure_ascii=False))
    
    if (attachments := message.attachments):
        attachment_part = [
            {
                'text': file.filename + ('\n' if index < len(attachments) else ''), 
                'italic': True, 
                'color': 'aqua',
                'clickEvent': {'action': 'open_url', 'value': file.url},
                'hoverEvent': {'action': 'show_text', 'value': file.url}
            } 
            for index, file in enumerate(attachments, 1)
        ]
        result.append('/tellraw @a ' + json.dumps(base_part + attachment_part, ensure_ascii=False))

    if (stickers := message.stickers):
        sticker_part = [{'text': f'*стикер* ({stickers[0].name})', 'italic': True}]
        result.append('/tellraw @a ' + json.dumps(base_part + sticker_part, ensure_ascii=False))

    return result


bot.run(token)