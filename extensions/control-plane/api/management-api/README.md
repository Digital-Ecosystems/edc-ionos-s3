# Management API

This group of modules provides the Management API. With this API you can manage different things of the connector.

## Authentication

The submodule `:extensions:control-plane:api:management-api:management-api-configuration` **requires**, that an implementation of the
`AuthenticationService` interface was registered. Therefor you have to add an authentication module to your dependencies
(e.g. `:extensions:common:auth:auth-tokenbased` or `:extensions:common:auth:auth-basic`).
