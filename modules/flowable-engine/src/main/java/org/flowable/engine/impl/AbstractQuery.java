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
package org.flowable.engine.impl;

import java.io.Serializable;
import java.util.List;

import org.flowable.engine.ManagementService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.query.Query;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.engine.common.impl.Direction;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.db.ListQueryParameterObject;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;

/**
 * Abstract superclass for all query types.
 *
 * @author Joram Barrez
 */
public abstract class AbstractQuery<T extends Query<?, ?>, U> extends ListQueryParameterObject implements Command<Object>, Query<T, U>, Serializable {

    private static final long serialVersionUID = 1L;

    protected transient CommandExecutor commandExecutor;
    protected transient CommandContext commandContext;

    protected AbstractQuery() {
        parameter = this;
    }

    protected AbstractQuery(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public AbstractQuery(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    // To be used by custom queries
    public AbstractQuery(ManagementService managementService) {
        this(((ManagementServiceImpl) managementService).getCommandExecutor());
    }

    public AbstractQuery<T, U> setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    public T orderBy(QueryProperty property) {
        this.orderProperty = property;
        return (T) this;
    }

    public T orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder) {
        orderBy(property);
        this.nullHandlingOnOrder = nullHandlingOnOrder;
        return (T) this;
    }

    public T asc() {
        return direction(Direction.ASCENDING);
    }

    public T desc() {
        return direction(Direction.DESCENDING);
    }

    @SuppressWarnings("unchecked")
    public T direction(Direction direction) {
        if (orderProperty == null) {
            throw new FlowableIllegalArgumentException("You should call any of the orderBy methods first before specifying a direction");
        }
        addOrder(orderProperty.getName(), direction.getName(), nullHandlingOnOrder);
        orderProperty = null;
        nullHandlingOnOrder = null;
        return (T) this;
    }

    protected void checkQueryOk() {
        if (orderProperty != null) {
            throw new FlowableIllegalArgumentException("Invalid query: call asc() or desc() after using orderByXX()");
        }
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
        return executeList(Context.getCommandContext());
    }

    @SuppressWarnings("unchecked")
    public List<U> listPage(int firstResult, int maxResults) {
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.resultType = ResultType.LIST_PAGE;
        if (commandExecutor != null) {
            return (List<U>) commandExecutor.execute(this);
        }
        return executeList(Context.getCommandContext());
    }

    public long count() {
        this.resultType = ResultType.COUNT;
        if (commandExecutor != null) {
            return (Long) commandExecutor.execute(this);
        }
        return executeCount(Context.getCommandContext());
    }

    public Object execute(CommandContext commandContext) {
        if (resultType == ResultType.LIST) {
            return executeList(commandContext);
        } else if (resultType == ResultType.SINGLE_RESULT) {
            return executeSingleResult(commandContext);
        } else if (resultType == ResultType.LIST_PAGE) {
            return executeList(commandContext);
        } else {
            return executeCount(commandContext);
        }
    }

    public abstract long executeCount(CommandContext commandContext);

    /**
     * Executes the actual query to retrieve the list of results.
     */
    public abstract List<U> executeList(CommandContext commandContext);

    public U executeSingleResult(CommandContext commandContext) {
        List<U> results = executeList(commandContext);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("Query return " + results.size() + " results instead of max 1");
        }
        return null;
    }

}
