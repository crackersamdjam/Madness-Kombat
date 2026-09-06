# Madness Kombat

This is a 2D local PVP shooter game made in Java for fun. The game is intended to be played on one computer with one or more keyboards, and also supports LAN multiplayer. This game took about a total of 3 months to make. No external game development libraries or engines are used; it is purely programmed in Java. All graphics are self-made. The sound effects are mostly from freesound.org while some of them are from Youtube. The main soundtrack is composed by me in Studio One 2 over the course of 2 months. This project can be used as a reference for an introduction to game development (e.g students who are taking a Computer Science course in highschool), but is definitely not a refined product.

## Features
  1. Game structure (menu, settings, gameplay, etc)
  2. Simple 2D collision and physics
  3. Usage of AWT graphics and textures
  4. Creating, editing, and loading a map
  5. Reading and writing game settings
  6. Sound effects and an original soundtrack (looped in-game)
  7. LAN multiplayer (host / join)
  8. Some bugs and errors

## How to Play
Download the zip file in the `Download` folder, extract it, and run the executable.

**Note:** keep the map files and the configs file in the same folder as the executable!

## Multiplayer
LAN play uses an authoritative host. The host runs the real match; other computers send input and draw the world the host broadcasts.

1. Every computer needs the same map file that the host selected in Options (keep maps next to the game, as usual).
2. On the host machine, open **Host Game**. The lobby shows your LAN IP and port `1331`.
3. On each other machine, open **Join Game**, enter that IP and a name, then connect.
4. When at least two players are in the lobby, the host presses Enter to start.

Online, each computer controls one fighter. Your keys are Player 1's controls from Options (arrows / shoot by default on a fresh config). The host uses the map, mode, teams, skins, and weapon / item pool from their Options. ESC leaves a joined match, or pauses on the host.
