package org.cratedb.action.sql;

import com.google.common.collect.ImmutableMap;
import org.apache.lucene.search.Query;
import org.cratedb.action.parser.*;
import org.cratedb.sql.parser.parser.NodeTypes;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.*;


public class ParsedStatement {

    private ESLogger logger = Loggers.getLogger(ParsedStatement.class);
    private String schemaName;
    public final ArrayList<Tuple<String, String>> outputFields = new ArrayList<>();
    private String[] indices = null;
    private boolean useFacet;
    private ActionType type;
    private int nodeType;

    private Map<String, Object> updateDoc;
    private Map<String, Object> plannerResults;
    private boolean countRequest;
    private boolean hasOrderBy = false;

    public IndexRequest[] indexRequests;
    public boolean hasVersionSysColumn;

    public String stmt;
    public Query query;

    public ParsedStatement(String stmt) {
        this.stmt = stmt;
    }

    public ImmutableMap<String, Object> indexSettings;
    public ImmutableMap<String, Object> indexMapping;

    public static enum ActionType {
        SEARCH_ACTION,
        INSERT_ACTION,
        DELETE_BY_QUERY_ACTION,
        BULK_ACTION, GET_ACTION,
        DELETE_ACTION,
        UPDATE_ACTION,
        CREATE_INDEX_ACTION,
        DELETE_INDEX_ACTION,
        MULTI_GET_ACTION,
        INFORMATION_SCHEMA_TABLES,
    }

    public static final int UPDATE_RETRY_ON_CONFLICT = 3;

    public BytesReference xcontent;

    public List<String> groupByColumnNames;
    public List<ColumnDescription> resultColumnList;

    public Integer limit = null;
    public Integer offset = null;

    public List<OrderByColumnIdx> orderByIndices;
    public OrderByColumnIdx[] orderByIndices() {
        if (orderByIndices != null) {
            return orderByIndices.toArray(new OrderByColumnIdx[orderByIndices.size()]);
        }

        return new OrderByColumnIdx[0];
    }

    public void schemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String schemaName() {
        return schemaName;
    }

    public void tableName(String tableName) {
        if (indices == null) {
            indices = new String[] { tableName };
        } else {
            indices[0] = tableName;
        }
    }

    public String tableName() {
        return indices[0];
    }

    public String[] indices() {
        return indices;
    }

    public void type(ActionType type) {
        this.type = type;
    }

    public ActionType type() {
        return type;
    }

    public void nodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public int nodeType() {
        return nodeType;
    }

    /**
     * Get the result column-names as listed in the SELECT Statement,
     * eventually including aliases, not real column-names
     * @return Array of Column-Name or -Alias Strings
     */
    public String[] cols() {
        String[] cols = new String[outputFields.size()];
        for (int i = 0; i < outputFields.size(); i++) {
            cols[i] = outputFields.get(i).v1();
        }
        return cols;
    }

    /**
     * Get the ColumnNames that are actually fetched from the table for this statement
     * @return Array of Column-Name Strings
     */
    public String[] columnNames() {
        String[] colNames = new String[outputFields.size()];
        for (int i=0; i < outputFields.size(); i++) {
            colNames[i] = outputFields.get(i).v2();
        }
        return colNames;
    }

    /**
     * @return boolean indicating if a facet is used to gather the result
     */
    public boolean useFacet() {
        // currently only the update statement uses facets
        return nodeType() == NodeTypes.UPDATE_NODE;
    }

    public Map<String, Object> updateDoc() {
        return updateDoc;
    }

    /**
     * returns the requested output fields as a list of tuples where
     * the left side is the alias and the right side is the column name
     *
     * @return list of tuples
     */
    public List<Tuple<String, String>> outputFields() {
        return outputFields;
    }

    /**
     * Adds an additional output field
     * @param alias the name under which the field will show up in the result
     * @param columnName the name of the column the value comes from
     */
    public void addOutputField(String alias, String columnName) {
        this.outputFields.add(new Tuple<>(alias, columnName));
    }

    public void updateDoc(Map<String, Object> updateDoc) {
        this.updateDoc = updateDoc;
    }

    public void countRequest(boolean countRequest) {
        this.countRequest = countRequest;
    }

    public boolean countRequest() {
        return !hasGroupBy() && countRequest;
    }

    public void setPlannerResult(String key, Object value) {
        plannerResults.put(key, value);
    }

    public Object getPlannerResult(String key) {
        return plannerResults.get(key);
    }
    public Object removePlannerResult(String key) {
        return plannerResults.remove(key);
    }

    public Map<String, Object> plannerResults() {
        return plannerResults;
    }

    public boolean hasGroupBy() {
        return (groupByColumnNames != null && groupByColumnNames.size() > 0);
    }

    public boolean hasOrderBy() {
        return hasOrderBy;
    }

    public void setHasOrderBy(boolean hasOrderBy) {
        this.hasOrderBy = hasOrderBy;
    }
}
