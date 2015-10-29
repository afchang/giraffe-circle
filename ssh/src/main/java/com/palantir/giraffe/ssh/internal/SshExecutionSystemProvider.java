/**
 * Copyright 2015 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.giraffe.ssh.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ProviderMismatchException;
import java.util.Map;

import com.palantir.giraffe.command.Command;
import com.palantir.giraffe.command.CommandContext;
import com.palantir.giraffe.command.CommandFuture;
import com.palantir.giraffe.command.ExecutionSystem;
import com.palantir.giraffe.command.ExecutionSystemNotFoundException;
import com.palantir.giraffe.command.spi.ExecutionSystemProvider;

/**
 * Provides access to remote execution systems using SSH.
 *
 * @author bkeyes
 */
public final class SshExecutionSystemProvider extends ExecutionSystemProvider {

    @Override
    public String getScheme() {
        return SshUris.getExecScheme();
    }

    @Override
    public ExecutionSystem newExecutionSystem(URI uri, Map<String, ?> env) throws IOException {
        SshUris.checkExecUri(uri);
        InternalSshSystemRequest request = new InternalSshSystemRequest(uri, env);
        if (request.isInsternalSource()) {
            // this is being requested as part of HostControlSystem creation
            return new SshExecutionSystem(this, request);
        } else {
            return SshHostControlSystem.builder(request)
                    .setFileSystem()
                    .setExecutionSystem(this)
                    .build()
                    .getExecutionSystem();
        }
    }

    @Override
    public ExecutionSystem getExecutionSystem(URI uri) {
        SshUris.checkExecUri(uri);
        throw new ExecutionSystemNotFoundException(uri.toString());
    }

    @Override
    public CommandFuture execute(Command command, CommandContext context) {
        SshCommand cmd = checkCommand(command);
        return cmd.getExecutionSystem().execute(cmd, context);
    }

    private SshCommand checkCommand(Command c) {
        if (checkNotNull(c, "command must be non-null") instanceof SshCommand) {
            return (SshCommand) c;
        } else {
            String type = c.getClass().getName();
            throw new ProviderMismatchException("incompatible with command of type " + type);
        }
    }
}
