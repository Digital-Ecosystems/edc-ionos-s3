# SQL Data Plane Store

Provides SQL persistence for data flow transfer state.

## Prerequisites

Please apply this [schema](docs/schema.sql) to your SQL database.

## Entity Diagram

```plantuml
@startuml
entity edc_data_plane {
  * process_id: string <<PK>>
  * state: integer
  --
}
@enduml

```

-->

## Configuration

| Key                           | Description                       | Mandatory | 
|:------------------------------|:----------------------------------|-----------|
| edc.datasource.dataplane.name | Datasource used by this extension | X         |
