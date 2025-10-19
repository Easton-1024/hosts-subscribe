package com.xyt.project.listener;

import java.io.IOException;

public class StringSelection implements java.awt.datatransfer.Transferable, java.awt.datatransfer.ClipboardOwner{
    private String data;

    public StringSelection(String data) {
        this.data = data;
    }

    @Override
    public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
        return new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
        return java.awt.datatransfer.DataFlavor.stringFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
        }
        return data;
    }

    @Override
    public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable contents) {
        // 不需要实现
    }

}
