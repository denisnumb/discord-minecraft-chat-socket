import json
import discord
from discord.ext import tasks
from model import (
    MinecraftChatClient, 
    MinecraftMessage, 
    MessageType
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
    if (not message.content 
        or message.channel != minecraft_channel 
        or message.author == bot.user
        ):
        return

    if chat_client.connected:
        await chat_client.send_message(prepare_tellraw_command(message))


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
                color=discord.Color.gold()
            )
        ) 

def prepare_tellraw_command(message: discord.Message) -> str:
    username = message.author.nick or message.author.name
    role_color = str(message.author.top_role.color)

    data = [
            '',
            {'text': '[discord]', 'bold': True, 'color': '#F1C40F'},
            {'text': ' <', 'color': 'white'},
            {'text': username, 'color': role_color},
            {'text': '> ', 'color': 'white'},
            {'text': message.content, 'color': 'white'}
        ]

    return '/tellraw @a ' + json.dumps(data, ensure_ascii=False)


bot.run(token)