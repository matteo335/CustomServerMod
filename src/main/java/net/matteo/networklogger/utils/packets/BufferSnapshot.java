package net.matteo.networklogger.utils.packets;

public record BufferSnapshot(int readerIndex, int writerIndex, int capacity, byte[] payload) {}