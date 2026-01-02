import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: "http://localhost:9080",
    realm: "microservices-realm",
    clientId: "react-client",
});

export default keycloak;
