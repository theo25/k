// Copyright (c) 2012-2014 K Team. All Rights Reserved.
package org.kframework.krun.ioserver.commands;

import org.kframework.krun.api.io.FileSystem;

import java.net.Socket;

public class CommandUnknown extends Command {


    public CommandUnknown(String[] args, Socket socket, FileSystem fs) { //, Long maudeId) {
        super(args, socket, fs); //, maudeId);
        // TODO Auto-generated constructor stub
    }

    public void run() {
        fail("unknown command");
    }

}
