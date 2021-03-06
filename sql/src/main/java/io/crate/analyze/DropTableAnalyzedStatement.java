/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.analyze;

import io.crate.exceptions.SchemaUnknownException;
import io.crate.exceptions.TableUnknownException;
import io.crate.metadata.ReferenceInfos;
import io.crate.metadata.TableIdent;
import io.crate.metadata.table.SchemaInfo;

public class DropTableAnalyzedStatement extends AbstractDropTableAnalyzedStatement {

    public DropTableAnalyzedStatement(ReferenceInfos referenceInfos, boolean ignoreNonExistentTable) {
        super(referenceInfos, ignoreNonExistentTable);
    }

    @Override
    public void table(TableIdent tableIdent) {
        SchemaInfo schemaInfo = referenceInfos.getSchemaInfo(tableIdent.schema());
        if (schemaInfo == null) {
            if (dropIfExists) {
                noop = true;
                return;
            }
            throw new SchemaUnknownException(tableIdent.schema());
        }
        if (schemaInfo.systemSchema()) {
            throw new UnsupportedOperationException(
                    String.format("cannot delete '%s'.", tableIdent.fqn()));
        }
        tableInfo = schemaInfo.getTableInfo(tableIdent.name());

        if (tableInfo == null) {
            this.noop = true;
        }
        if (tableInfo == null && !dropIfExists) {
            throw new TableUnknownException(tableIdent.fqn());
        } else if (tableInfo != null && tableInfo.isAlias() && !tableInfo.isPartitioned()) {
            throw new UnsupportedOperationException("Table alias not allowed in DROP TABLE statement.");
        }
        this.tableIdent = tableIdent;
    }

    @Override
    public <C, R> R accept(AnalyzedStatementVisitor<C, R> analyzedStatementVisitor, C context) {
        return analyzedStatementVisitor.visitDropTableStatement(this, context);
    }
}
