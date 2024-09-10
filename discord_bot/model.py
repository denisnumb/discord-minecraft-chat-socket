import re
import socket
import asyncio
import discord
import json
from typing import Union
from threading import Thread
from enum import Enum

class MessageType(Enum):
	Message = 0
	PlayerJoin = 1
	PlayerLeft = 2
	PlayerDie = 3
	AdvancementMade = 4

class MinecraftMessage:
	def __init__(self, message_type: str, player_name: str, message: str) -> None:
		self.type = MessageType(int(message_type))
		self.player_name = player_name
		self.content = message

class BaseSocket:
	def receive_data(self, sender: socket.socket) -> bytes:
		l = int.from_bytes(self.receive_part(sender, 2), "big")
		return self.receive_part(sender, l)

	def receive_part(self, sender: socket.socket, to_receive: int) -> bytes:
		received = b''
		while len(received) < to_receive:
			chunk = sender.recv(min(to_receive - len(received), 2048))
			if chunk == b'':
				raise RuntimeError('Connection lost')
			received += chunk
		return received

	def send(self, receiver: socket.socket, data: bytes) -> None:
		receiver.send(len(data).to_bytes(2, 'big'))
		receiver.send(data)

class MinecraftChatClient(BaseSocket):
	def __init__(
		self, 
		ip: str, 
		port: int, 
		bot: discord.Bot, 
		on_message: callable
		) -> None:
		self.ip = ip
		self.port = port
		self.bot = bot
		self.on_message = on_message
		self.connected = False
		self.lock = asyncio.Lock()

	async def connect(self) -> None:
		async with self.lock:
			if not self.connected:
				await self.try_connect()
				Thread(target=self.receive_minecraft_messages).start()

	async def try_connect(self) -> bool:
		try:
			self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			print(f'[MinecraftChatClient] Подключение к {self.ip}:{self.port}...')
			self.sock.connect((self.ip, self.port))
			print(f'[MinecraftChatClient] Подключен к серверу [{self.ip}:{self.port}]')
			self.connected = True
		except Exception as e:
			print(f'[MinecraftChatClient] Не удалось подключиться к серверу: {e}')
			self.connected = False

	async def close_connection(self, exception: Exception) -> None:
		if self.connected:
			print(f'[MinecraftChatClient] Подключение разорвано: {exception}')
			self.sock.close()
			self.connected = False

	async def send_request(self, message: str) -> None:
		if not self.connected:
			return
		try:
			self.send(self.sock, bytes(message, 'utf-8'))
		except Exception as e:
			await self.close_connection(e)

	def receive_minecraft_messages(self) -> None:
		try:
			while True:
				raw_data = json.loads(self.receive_data(self.sock).decode('utf-8'))
				asyncio.run_coroutine_threadsafe(
					self.on_message(MinecraftMessage(**raw_data)), 
					self.bot.loop
				)
		except Exception as e:
			asyncio.run_coroutine_threadsafe(self.close_connection(e), self.bot.loop)

class MarkdownToken:
	@property
	def is_url(self) -> bool:
		return self.url is not None

	@property
	def has_no_markdown(self) -> bool:
		return not any((self.url, self.bold, self.italic, self.underlined, self.strikethrough, self.obfuscated, self.mention))

	@property
	def inner_tokens(self) -> list['MarkdownToken']:
		return self.__inner_tokens

	@inner_tokens.setter
	def inner_tokens(self, tokens: list['MarkdownToken']) -> None:
		self.__inner_tokens = tokens
		self.update_styles()

	def __init__(
			self,
			raw_text: str, 
			text: str=None,
			*,
			url: str=None,
			bold: bool=False, 
			italic: bool=False, 
			underlined: bool=False, 
			strikethrough: bool=False, 
			obfuscated: bool=False,
			mention: bool=False
			) -> None:
		self.raw_text = raw_text
		self.text = text or raw_text
		self.url = url
		self.bold = bold
		self.italic = italic
		self.underlined = underlined
		self.strikethrough = strikethrough
		self.obfuscated = obfuscated
		self.mention = mention
		self.__inner_tokens = []

	def __str__(self) -> str:
		result = f'<raw_text="{self.raw_text}", text="{self.text}"'

		if self.url:
			result += f', url={self.url}'

		styles = ", ".join(f"{attr}={value}" for attr in ('bold', 'italic', 'underlined', 'strikethrough', 'obfuscated', 'mention') if (value := getattr(self, attr)))

		if styles:
			result += ', ' + styles

		result += '>'
		return result

	def update_styles(self) -> None:
		for inner_token in self.__inner_tokens:
			inner_token.combine_styles(self)
			inner_token.update_styles()

	def combine_styles(self, another: 'MarkdownToken') -> None:
		self.url = self.url or another.url
		self.bold |= another.bold
		self.italic |= another.italic
		self.underlined |= another.underlined
		self.strikethrough |= another.strikethrough
		self.obfuscated |= another.obfuscated


class MD(Enum):
	LINK = ['link']                                 # ссылки [текст](url)
	UNDERLINED_ITALIC = ['underlined', 'italic']    # курсивный и подчеркнутый текст ___текст___
	UNDERLINED = ['underlined']                     # подчеркнутый текст __текст__
	ITALIC_underline = ['italic']                   # курсивный текст _текст_
	BOLD_ITALIC = ['bold', 'italic']                # жирный и курсивный текст ***текст***
	BOLD = ['bold']                                 # жирный текст **текст**
	ITALIC_star = ['italic']                        # курсивный текст *текст*
	STRIKETHROUGH = ['strikethrough']               # зачеркнутый текст ~~текст~~
	OBFUSCATED = ['obfuscated']                     # спойлер ||текст||
	URL = ['url']                                   # просто URL https://vk.com/
	DISCORD_MENTION = ['mention']					# упоминание в дискорде <@id> / <@$id> / <#id>

class MatchPattern:
	def __init__(self, style: MD, match: re.Match):
		self.style = style
		self.start = match.span()[0]
		self.match = match

class MentionData:
	def __init__(self, obj: Union[discord.Member, discord.Role, discord.TextChannel, discord.VoiceChannel]):
		self.name = obj.display_name if hasattr(obj, 'display_name') else obj.name
		self.pretty_mention = obj.mention[1] + self.name
		self.color = str(obj.color) if hasattr(obj, 'color') else '#6974c9'

patterns = [
		(r'(?<!\\)\[(.+?)\]\((https?://\S+)\)', MD.LINK),     
		(r'(?<!\\)_(?<!\\)_(?<!\\)_(.+?)(?<!\\)_(?<!\\)_(?<!\\)_', MD.UNDERLINED_ITALIC),        
		(r'(?<!\\)_(?<!\\)_(.+?)(?<!\\)_(?<!\\)_', MD.UNDERLINED),                 
		(r'(?<!\\)_(.+?)(?<!\\)_', MD.ITALIC_underline),             
		(r'(?<!\\)\*(?<!\\)\*(?<!\\)\*(.+?)(?<!\\)\*(?<!\\)\*(?<!\\)\*', MD.BOLD_ITALIC),        
		(r'(?<!\\)\*(?<!\\)\*(.+?)(?<!\\)\*(?<!\\)\*', MD.BOLD),                   
		(r'(?<!\\)\*(.+?)(?<!\\)\*', MD.ITALIC_star),                
		(r'(?<!\\)~(?<!\\)~(.+?)(?<!\\)~(?<!\\)~', MD.STRIKETHROUGH),              
		(r'(?<!\\)\|(?<!\\)\|(.+?)(?<!\\)\|(?<!\\)\|', MD.OBFUSCATED),             
		(r'(https?://\S+)', MD.URL),
		(r'(?<!\\)<((?<!\\)([@#][!&]?\d+)|(:.+?:\d+))(?<!\\)>', MD.DISCORD_MENTION)             
	]

except_patterns = {
	MD.UNDERLINED_ITALIC: [MD.UNDERLINED, MD.ITALIC_underline],
	MD.BOLD_ITALIC: [MD.BOLD, MD.ITALIC_star],
	MD.UNDERLINED: [MD.ITALIC_underline],
	MD.BOLD: [MD.ITALIC_star]
}

def parse_markdown(raw_text: str) -> list[MarkdownToken]:
	tokens: list[MarkdownToken] = []
	current_pos = 0

	def unescape_special_characters(text: str) -> str:
		return re.sub(r'\\([*_~|@><])', r'\1', text)

	def add_text_part(text: str) -> None:
		if text:
			tokens.append(MarkdownToken(unescape_special_characters(text)))

	while current_pos < len(raw_text):
		match_pattern: MatchPattern = None

		for pattern, style in patterns:
			if (match := re.search(pattern, raw_text[current_pos:])):
				if match_pattern:
					if ((match_pattern.style in except_patterns and style in except_patterns[match_pattern.style] and match_pattern.start == match.span()[0])
						or (match_pattern.start < match.span()[0])
						):
						continue

				match_pattern = MatchPattern(style, match)

		if not match_pattern:
			add_text_part(raw_text[current_pos:])
			break

		style = match_pattern.style
		match = match_pattern.match
		start, end = match.span()

		add_text_part(raw_text[current_pos:current_pos + start])

		matched_text = match.group(0)
		inner_text = match.group(1) if len(match.groups()) > 0 else None
		
		token: MarkdownToken = None
		if style == MD.URL:
			token = MarkdownToken(matched_text, url=matched_text)
		elif style == MD.LINK:
			token = MarkdownToken(matched_text, inner_text, url=match.group(2))
		elif style == MD.DISCORD_MENTION:
			token = MarkdownToken(matched_text, mention=True)
		else:
			token = MarkdownToken(matched_text, inner_text)
			for attr_name in style.value:
				setattr(token, attr_name, True)

		if token.raw_text != token.text and (inner_tokens := [t for t in parse_markdown(token.text) if not t.has_no_markdown]):
			token.inner_tokens = inner_tokens
		
		token.text = unescape_special_characters(token.text)
		tokens.append(token)

		current_pos += end
	
	return tokens


def convert_tokens_to_json(tokens: list[MarkdownToken], mentions: dict[str, MentionData]={}) -> list[dict]:
	result = []

	def add_part(token: MarkdownToken, text_part: str) -> None:
		part = {'text': text_part}

		if text_part in mentions:
			part['text'] = mentions[text_part].pretty_mention
			part['color'] = mentions[text_part].color
			text_part = part['text']

		if text_part.strip():
			for attr in ('bold', 'italic', 'strikethrough', 'underlined', 'obfuscated'):
				if (value := getattr(token, attr)):
					part[attr] = value

			if token.obfuscated:
				part['hoverEvent'] = {'action': 'show_text', 'value': text_part}

			if token.is_url:
				part['color'] = 'aqua'
				part['clickEvent'] = {'action': 'open_url', 'value': token.url}
				hover_value = token.url if not token.obfuscated else f'{text_part} ({token.url})'
				part['hoverEvent'] = {'action': 'show_text', 'value': hover_value}

		result.append(part)

	def convert(token: MarkdownToken) -> None:
		if token.inner_tokens:
			current_pos = 0

			for inner_token in token.inner_tokens:
				start_idx = re.search(re.escape(inner_token.raw_text), token.text[current_pos:]).start() + current_pos
				
				if start_idx > current_pos:
					text_part = token.text[current_pos:start_idx].strip('_*~|')
					add_part(token, text_part)

				convert(inner_token)
				current_pos = start_idx + len(inner_token.raw_text)

			if current_pos < len(token.text):
				text_part = token.text[current_pos:].strip('_*~|')
				if text_part.strip():
					add_part(token, text_part)
		else:
			add_part(token, token.text)

	for token in tokens:
		convert(token)

	return result