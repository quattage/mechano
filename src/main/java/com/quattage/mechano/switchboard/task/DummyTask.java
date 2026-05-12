package com.quattage.mechano.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.switchboard.action.ActionTask;
import com.quattage.mechano.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;

public class DummyTask implements ActionTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class<?>[0];
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        
    }

    @Override
    public @Nullable Object[] dynamicDecode(ByteBuf buffer) {
        return new Class<?>[0];
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        return GridAction.RESPONSE_SUCCESS;
    }
    
}
