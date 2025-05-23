import {
  addMongoDatabase,
  addMySQLDatabase,
  addPostgresDatabase,
  restore,
  setupWritableDB,
  snapshot,
} from "e2e/support/helpers";

describe("qa databases snapshots", { tags: "@external" }, () => {
  beforeEach(() => {
    restoreAndAuthenticate();
  });

  it("creates snapshots for supported qa databases", () => {
    if (Cypress.env("QA_DB_MONGO") === true) {
      addMongoDatabase();
      snapshot("mongo-5");
      deleteDatabase("mongoID");

      restoreAndAuthenticate();
    } else {
      addPostgresDatabase();
      snapshot("postgres-12");
      deleteDatabase("postgresID");

      restoreAndAuthenticate();

      setupWritableDB("postgres");
      addPostgresDatabase("Writable Postgres12", true);
      snapshot("postgres-writable");
      deleteDatabase("postgresID");

      restoreAndAuthenticate();

      addMySQLDatabase({});
      snapshot("mysql-8");
      deleteDatabase("mysqlID");

      restoreAndAuthenticate();

      setupWritableDB("mysql");
      addMySQLDatabase({ displayName: "Writable MySQL8", writable: true });
      snapshot("mysql-writable");
      deleteDatabase("mysqlID");

      restoreAndAuthenticate();
    }

    restore("blank");
  });
});

function restoreAndAuthenticate() {
  restore("default");
  cy.signInAsAdmin();
}

function deleteDatabase(idAlias) {
  cy.get("@" + idAlias).then((id) => {
    return cy.request("DELETE", `/api/database/${id}`);
  });
}
