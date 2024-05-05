# Whiteboard – Multiuser System

## Requirements 1: Basic Features

- Multiple users can draw on a shared interactive canvas.
- The system will support a single whiteboard shared between all clients.
- Key Elements with GUI:
    - Shapes: The whiteboard should support lines, circles, ovals, and rectangles.
    - Free draw and erase functionality (multiple sizes of eraser are convenient).
    - Text input: Users can type text anywhere inside the whiteboard.
- Users can choose their favorite drawing color from at least 16 available colors.
- Creativity and innovation are encouraged.

## Requirements 2: Advanced Features

1. Chat Window (text-based): Users can communicate with each other by typing messages.
2. "File" menu with options for new, open, save, saveAs, and close (manager control only).
3. Manager can kick out a certain peer/user from the whiteboard.

## Service Challenges

- Dealing with concurrency:
    - Properly handling access to shared resources.
    - Ensuring simultaneous actions lead to a reasonable state.
- Structuring the application and handling system state:
    - Multiple servers or a single central one managing all system state.
- Dealing with networked communication:
    - Deciding message sending across the network.
    - Designing an exchange protocol for messages and replies.
    - Designing remote interfaces and servants if using RMI.
- Implementing the GUI:
    - Functionality similar to tools like MS Paint.
    - Use of any tool/API/library, e.g., Java2D drawing package.

## Development Specifications

Develop a whiteboard that can be shared between multiple users over the network.

- Implemented in Java, with freedom to choose technology (e.g., Sockets) for the distributed application.
- Choice of Sockets, TCP, or UDP.
- Definition of message format and exchange protocol (XML-based or custom).
- Clients can broadcast message updates to all other clients, with acknowledgments.
- Consideration of Java RMI, remote objects/interfaces.
- Choice of file or database for storage.
- Use of technology of choice, ensuring the selected technology can achieve the goal. If uncertain, stick to familiar technologies.