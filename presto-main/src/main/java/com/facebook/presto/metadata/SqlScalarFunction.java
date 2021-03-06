/*
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
package com.facebook.presto.metadata;

import com.facebook.presto.operator.scalar.ScalarFunctionImplementation;
import com.facebook.presto.spi.type.TypeManager;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;

import static com.facebook.presto.metadata.FunctionKind.SCALAR;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public abstract class SqlScalarFunction
        implements SqlFunction
{
    private final Signature signature;

    public static SqlScalarFunction create(
            Signature signature,
            String description,
            boolean hidden,
            MethodHandle methodHandle,
            Optional<MethodHandle> instanceFactory,
            boolean deterministic,
            boolean nullable,
            List<Boolean> nullableArguments)
    {
        return new SimpleSqlScalarFunction(
                signature,
                description,
                hidden,
                methodHandle,
                instanceFactory,
                deterministic,
                nullable,
                nullableArguments);
    }

    protected SqlScalarFunction(Signature signature)
    {
        this.signature = requireNonNull(signature, "signature is null");
        checkArgument(signature.getKind() == SCALAR, "function kind must be SCALAR");
    }

    @Override
    public final Signature getSignature()
    {
        return signature;
    }

    public abstract ScalarFunctionImplementation specialize(BoundVariables boundVariables, int arity, TypeManager typeManager, FunctionRegistry functionRegistry);

    public static SqlScalarFunctionBuilder builder(Class<?> clazz)
    {
        return new SqlScalarFunctionBuilder(clazz);
    }

    private static class SimpleSqlScalarFunction
            extends SqlScalarFunction
    {
        private final MethodHandle methodHandle;
        private final Optional<MethodHandle> instanceFactory;
        private final String description;
        private final boolean hidden;
        private final boolean nullable;
        private final List<Boolean> nullableArguments;
        private final boolean deterministic;

        public SimpleSqlScalarFunction(
                Signature signature,
                String description,
                boolean hidden,
                MethodHandle methodHandle,
                Optional<MethodHandle> instanceFactory,
                boolean deterministic,
                boolean nullable,
                List<Boolean> nullableArguments)
        {
            super(signature);
            checkArgument(signature.getTypeVariableConstraints().isEmpty(), "%s is parametric", signature);
            this.description = description;
            this.hidden = hidden;
            this.methodHandle = requireNonNull(methodHandle, "methodHandle is null");
            this.instanceFactory = requireNonNull(instanceFactory, "instanceFactory is null");
            this.deterministic = deterministic;
            this.nullable = nullable;
            this.nullableArguments = requireNonNull(nullableArguments, "nullableArguments is null");
        }

        @Override
        public boolean isHidden()
        {
            return hidden;
        }

        @Override
        public boolean isDeterministic()
        {
            return deterministic;
        }

        @Override
        public String getDescription()
        {
            return description;
        }

        @Override
        public ScalarFunctionImplementation specialize(BoundVariables boundVariables, int arity, TypeManager typeManager, FunctionRegistry functionRegistry)
        {
            return new ScalarFunctionImplementation(nullable, nullableArguments, methodHandle, instanceFactory, isDeterministic());
        }
    }
}
