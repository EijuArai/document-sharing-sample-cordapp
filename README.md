<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Document Sharing Sample CorDapp

This is a sample [Corda](https://corda.net) CorDapp written in Kotlin that demonstrates a document sharing and approval workflow between two parties on a Corda network.

A **proposer** can propose a document to a **consenter**. The consenter can then approve or reject the document. If rejected, the proposer can revise and re-submit the document. All state transitions are recorded immutably on the shared ledger.

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Project Structure

```
document-sharing-sample-cordapp/
├── contracts/   # DocumentState, DocumentContract, DocumentTypes
├── workflows/   # Flows: ProposeDocument, ApproveDocument, RejectDocument, ReviseDocument
└── clients/     # RPC command-line client and Spring Boot webserver
```

# CorDapp Overview

## State: DocumentState

`DocumentState` (`contracts/src/main/kotlin/com/template/states/DocumentState.kt`) is a `LinearState` that represents a shared document on the ledger. It holds the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `proposer` | `Party` | The party who proposes the document |
| `consenter` | `Party` | The party who must approve or reject the document |
| `documentTitle` | `String` | Title of the document |
| `documentType` | `DocumentTypes` | Type of the document (see below) |
| `documentJson` | `String` | Document content as a JSON string |
| `comments` | `String?` | Optional comments (required when rejecting) |
| `versionNo` | `Int` | Version counter, starts at `0` and increments on each update |
| `status` | `DocumentStatus` | Current status of the document (see below) |
| `updateTime` | `Date` | Timestamp of the last state change |
| `linearId` | `UniqueIdentifier` | Unique identifier for the document across all versions |

### DocumentStatus

| Value | Description |
|-------|-------------|
| `PROPOSED` | Document has been proposed and is awaiting a decision |
| `APPROVED` | Document has been approved by the consenter |
| `REJECTED` | Document has been rejected by the consenter |

### DocumentTypes

| Value | Description |
|-------|-------------|
| `GENSEN` | Gensen document type |
| `TAISHOKU` | Taishoku document type |
| `NAITEI` | Naitei document type |

## Contract: DocumentContract

`DocumentContract` (`contracts/src/main/kotlin/com/template/contracts/DocumentContract.kt`) enforces the rules for each state transition. It supports four commands:

### Propose
- No input `DocumentState`; one output `DocumentState`
- Output status must be `PROPOSED`, version must be `0`
- Proposer and consenter must be different parties
- Both proposer and consenter must sign

### Approve
- One input `DocumentState` with status `PROPOSED`; one output `DocumentState`
- Output status must be `APPROVED`, version must be incremented
- Proposer, consenter and document content must remain unchanged
- Both proposer and consenter must sign

### Reject
- One input `DocumentState` with status `PROPOSED`; one output `DocumentState`
- Output status must be `REJECTED`, version must be incremented
- `comments` must be provided
- Proposer, consenter and document content must remain unchanged
- Both proposer and consenter must sign

### Revise
- One input `DocumentState` with status `PROPOSED` or `REJECTED`; one output `DocumentState`
- Output status must return to `PROPOSED`, version must be incremented
- Document title, type, content and comments may be updated
- Proposer and consenter must remain unchanged
- Both proposer and consenter must sign

## Document Lifecycle

```
[ProposeDocument] ──► PROPOSED ──► [ApproveDocument] ──► APPROVED
                          │  ▲
                          │  └─────────────────[ReviseDocument]
                          ▼                             ▲
                  [RejectDocument] ──► REJECTED ────────┘
```

Revision is allowed from both `PROPOSED` and `REJECTED` states.

## Flows

### ProposeDocument
Initiated by the **proposer** node. Note: `documentType` is passed as a `String` (e.g. `"NAITEI"`) and converted to the `DocumentTypes` enum internally.

```
ProposeDocument(
    proposer: Party,
    consenter: Party,
    documentTitle: String,
    documentType: String,   // one of: GENSEN, TAISHOKU, NAITEI
    documentJson: String,
    comments: String?       // optional
)
```

### ApproveDocument
Initiated by the **consenter** node. The document must have status `PROPOSED`.

```
ApproveDocument(
    linearId: UniqueIdentifier,
    comments: String?           // optional
)
```

### RejectDocument
Initiated by the **consenter** node. The document must have status `PROPOSED`. `comments` are required.

```
RejectDocument(
    linearId: UniqueIdentifier,
    comments: String            // required; contract verification fails if null or empty
)
```

### ReviseDocument
Initiated by the **proposer** node. The document must have status `PROPOSED` or `REJECTED`. Note: unlike `ProposeDocument`, `documentType` takes the `DocumentTypes` enum value directly.

```
ReviseDocument(
    linearId: UniqueIdentifier,
    documentTitle: String,
    documentType: DocumentTypes,  // enum value: GENSEN, TAISHOKU, or NAITEI
    documentJson: String,
    comments: String?             // optional
)
```

# Usage

## Building and Deploying the Nodes

Run the `deployNodes` Gradle task to compile the CorDapp and create a local test network of four nodes under `build/nodes/`:

```bash
./gradlew deployNodes
```

The network consists of the following nodes:

| Node | X.500 Name | P2P Port | RPC Port |
|------|-----------|----------|----------|
| Notary | `O=Notary,L=London,C=GB` | 10002 | 10003 |
| NodeA | `O=NodeA,L=Tokyo,C=JP` | 10005 | 10006 |
| NodeB | `O=NodeB,L=Tokyo,C=JP` | 10008 | 10009 |
| NodeC | `O=NodeC,L=Tokyo,C=JP` | 10011 | 10012 |

RPC credentials for NodeA, NodeB, and NodeC: username `user1`, password `test`.

## Starting the Nodes

```bash
# Linux / macOS
./build/nodes/runnodes

# Windows
.\build\nodes\runnodes.bat
```

This opens a separate terminal window for each node.

## Interacting with the Nodes

### Shell

When started via the command line, each node displays an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue Nov 06 11:58:13 GMT 2018>>>

You can use this shell to interact with your node. For example, enter `run networkMapSnapshot` to see a list of
the other nodes on the network:

    Tue Nov 06 11:58:13 GMT 2018>>> run networkMapSnapshot
    [
      {
      "addresses" : [ "localhost:10002" ],
      "legalIdentitiesAndCerts" : [ "O=Notary, L=London, C=GB" ],
      "platformVersion" : 11,
      "serial" : 1541505484825
    },
      {
      "addresses" : [ "localhost:10005" ],
      "legalIdentitiesAndCerts" : [ "O=NodeA, L=Tokyo, C=JP" ],
      "platformVersion" : 11,
      "serial" : 1541505382560
    },
      {
      "addresses" : [ "localhost:10008" ],
      "legalIdentitiesAndCerts" : [ "O=NodeB, L=Tokyo, C=JP" ],
      "platformVersion" : 11,
      "serial" : 1541505384742
    },
      {
      "addresses" : [ "localhost:10011" ],
      "legalIdentitiesAndCerts" : [ "O=NodeC, L=Tokyo, C=JP" ],
      "platformVersion" : 11,
      "serial" : 1541505386910
    }
    ]

    Tue Nov 06 12:30:11 GMT 2018>>>

You can find out more about the node shell [here](https://docs.corda.net/shell.html).

#### Propose a document (from NodeA shell)

```
flow start ProposeDocument proposer: "O=NodeA,L=Tokyo,C=JP", consenter: "O=NodeB,L=Tokyo,C=JP", documentTitle: "Sample Contract", documentType: NAITEI, documentJson: "{\"key\":\"value\"}"
```

#### Approve a document (from NodeB shell)

First retrieve the `linearId` of the document from the vault:

```
run vaultQuery contractStateType: com.template.states.DocumentState
```

Then approve using the `linearId` printed in the output:

```
flow start ApproveDocument linearId: <linearId>, comments: "Looks good."
```

#### Reject a document (from NodeB shell)

```
flow start RejectDocument linearId: <linearId>, comments: "Please revise section 2."
```

#### Revise a document (from NodeA shell)

```
flow start ReviseDocument linearId: <linearId>, documentTitle: "Sample Contract v2", documentType: NAITEI, documentJson: "{\"key\":\"updated\"}", comments: "Updated section 2."
```

### Running Tests Inside IntelliJ

We recommend editing your IntelliJ preferences so that you use the Gradle runner — this ensures that the quasar utils
plugin sets the required flags (such as ``-javaagent``) automatically.

To switch to using the Gradle runner:

* Navigate to ``Build, Execution, Deployment -> Build Tools -> Gradle -> Runner`` (or search for `runner`)
  * Windows: this is in "Settings"
  * MacOS: this is in "Preferences"
* Set "Delegate IDE build/run actions to gradle" to true
* Set "Run test using:" to "Gradle Test Runner"

If you would prefer to use the built-in IntelliJ JUnit test runner, run ``gradlew installQuasar`` to copy the quasar
JAR file to the lib directory, then specify ``-javaagent:lib/quasar.jar`` and set the run directory to the project
root directory for each test.

### Client

`clients/src/main/kotlin/com/template/Client.kt` defines a simple command-line client that connects to a node via RPC
and prints a list of the other nodes on the network.

#### Running the client

##### Via the command line

Run the `runTemplateClient` Gradle task. By default, it connects to NodeA with RPC address `localhost:10006` using
username `user1` and password `test`.

##### Via IntelliJ

Run the `Run Template Client` run configuration. By default, it connects to NodeA with RPC address `localhost:10006`
using username `user1` and password `test`.

### Webserver

`clients/src/main/kotlin/com/template/webserver/` defines a Spring Boot webserver that connects to a node via RPC and
allows you to interact with the node over HTTP.

The API endpoints are defined here:

     clients/src/main/kotlin/com/template/webserver/Controller.kt

#### Running the webserver

##### Via the command line

Run the `runTemplateServer` Gradle task. By default, it connects to NodeA with RPC address `localhost:10006` using
username `user1` and password `test`, and serves the webserver on port `localhost:10050`.

##### Via IntelliJ

Run the `Run Template Server` run configuration. By default, it connects to NodeA with RPC address `localhost:10006`
using username `user1` and password `test`, and serves the webserver on port `localhost:10050`.

#### Interacting with the webserver

The webserver is served on:

    http://localhost:10050
