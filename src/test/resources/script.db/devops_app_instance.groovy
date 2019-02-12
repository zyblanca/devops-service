package script.db

databaseChangeLog(logicalFilePath: 'dba/devops_app_instance.groovy') {
    changeSet(author: 'Zenger', id: '2018-04-12-create-table') {
        createTable(tableName: "devops_app_instance", remarks: '实例') {
            column(name: 'id', type: 'BIGINT UNSIGNED', remarks: '主键，ID', autoIncrement: true) {
                constraints(primaryKey: true)
            }
            column(name: 'code', type: 'VARCHAR(64)', remarks: '实例code')
            column(name: 'app_id', type: 'BIGINT UNSIGNED', remarks: '应用 ID')
            column(name: 'app_version_id', type: 'BIGINT UNSIGNED', remarks: '应用版本 ID')
            column(name: 'env_id', type: 'BIGINT UNSIGNED', remarks: '环境 ID')
            column(name: 'status', type: 'VARCHAR(32)', remarks: '实例状态')

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }

        addUniqueConstraint(tableName: 'devops_app_instance',
                constraintName: 'uk_code', columnNames: 'code')

        createIndex(indexName: "idx_status ", tableName: "devops_app_instance") {
            column(name: "status")
        }
    }


    changeSet(author: 'younger', id: '2018-09-10-add-column')
            {
                addColumn(tableName: 'devops_app_instance') {
                    column(name: 'command_id', type: 'BIGINT UNSIGNED', remarks: 'command id', afterColumn: 'env_id')
                }
            }

}