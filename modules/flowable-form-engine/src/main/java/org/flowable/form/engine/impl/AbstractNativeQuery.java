/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.form.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.engine.common.BaseNativeQuery;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.query.NativeQuery;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;

/**
 * Abstract superclass for all native query types.
 * 
 * @author Tijs Rademakers
 */
public abstract class AbstractNativeQuery<T extends NativeQuery<?, ?>, U> extends BaseNativeQuery<T, U> implements Command<Object> {

    private static final long serialVersionUID = 1L;

    protected transient CommandExecutor commandExecutor;
    protected transient CommandContext commandContext;

    protected AbstractNativeQuery(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public AbstractNativeQuery(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    public AbstractNativeQuery<T, U> setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T sql(String sqlStatement) {
        this.sqlStatement = sqlStatement;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T parameter(String name, Object value) {
        parameters.put(name, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public U singleResult() {
        this.resultType = ResultType.SINGLE_RESULT;
        if (commandExecutor != null) {
            return (U) commandExecutor.execute(this);
        }
        return executeSingleResult(Context.getCommandContext());
    }

    @SuppressWarnings("unchecked")
    public List<U> list() {
        this.resultType = ResultType.LIST;
        if (commandExecutor != null) {
            return (List<U>) commandExecutor.execute(this);
        }
        return executeList(Context.getCommandContext(), generateParameterMap());
    }

    @SuppressWarnings("unchecked")
    public List<U> listPage(int firstResult, int maxResults) {
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.resultType = ResultType.LIST_PAGE;
        if (commandExecutor != null) {
            return (List<U>) commandExecutor.execute(this);
        }
        return executeList(Context.getCommandContext(), generateParameterMap());
    }

    public long count() {
        this.resultType = ResultType.COUNT;
        if (commandExecutor != null) {
            return (Long) commandExecutor.execute(this);
        }
        return executeCount(Context.getCommandContext(), generateParameterMap());
    }

    public Object execute(CommandContext commandContext) {
        if (resultType == ResultType.LIST) {
            return executeList(commandContext, generateParameterMap());
        } else if (resultType == ResultType.LIST_PAGE) {
            return executeList(commandContext, generateParameterMap());
        } else if (resultType == ResultType.SINGLE_RESULT) {
            return executeSingleResult(commandContext);
        } else {
            return executeCount(commandContext, generateParameterMap());
        }
    }

    public abstract long executeCount(CommandContext commandContext, Map<String, Object> parameterMap);

    /**
     * Executes the actual query to retrieve the list of results.
     * 
     * @param maxResults
     * @param firstResult
     *
     */
    public abstract List<U> executeList(CommandContext commandContext, Map<String, Object> parameterMap);

    public U executeSingleResult(CommandContext commandContext) {
        List<U> results = executeList(commandContext, generateParameterMap());
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("Query return " + results.size() + " results instead of max 1");
        }
        return null;
    }

}
