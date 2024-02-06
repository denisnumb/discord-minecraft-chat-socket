import socket
import asyncio
import discord
import json
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