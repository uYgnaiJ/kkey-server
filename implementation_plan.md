# Password Management System Design Plan

A multiplatform, resilient, and recoverable password management system designed for ease of use over extreme security.

## Proposed Design

### 1. System Architecture
The system consists of a **Multiplatform Client** and a network of **Interconnected Servers**.

- **Client**: Built with **Compose Multiplatform**. It maintains a local list of server URLs and dynamically discovers new ones from the servers it connects to.
- **Server**: Lightweight Java/Kotlin service (e.g., Ktor).
- **Discovery**: When a client connects to Server A, Server A returns its known active peers. The client adds these to its local contact list for future fallback.
- **Network**: A Mesh architecture where servers synchronize data.

---

### 2. Data Model & Storage
- **Structure**: JSON documents.
- **Schema (`PasswordEntry`)**:
  - `id`: Unique identifier (UUID).
  - `name`: Name of the entry (e.g., "Google account").
  - `password`: The sensitive data (stored/encrypted).
  - `description`: Optional text for notes/hints.
  - `url`: Where this password is used.
  - `lastModified`: Unix timestamp.
  - `version`: For conflict resolution during sync.

---

### 3. Synchronization Strategy
- **Conflict-Free Replicated Data Types (Small scale)**: Since we are dealing with key-value pairs, we use a simple **vector clock** or **lamport timestamp** to track changes.
- **Peer Discovery**: Servers are configured with a list of "Initial Peers". Once a server joins the cluster, it asks peers for their current peer lists, building a local mesh map.
- **Delta Sync**: When a client pushes a change to Server A, Server A broadcasts the change (the "delta") to all known peers. If a peer is offline, it will fetch missing updates using its last known sequence number when it comes back online.
- **Resilience**: The client keeps a list of all servers. If Server A fails, it automatically retries with Server B.

---

### 4. Recovery & Authentication (The "Ease of Use" Trade-off)
> [!IMPORTANT]
> To ensure recovery without a master key, we shift the "Source of Truth" encryption to the server-side or provide an admin-level override.

- **Client Authentication**: Local PIN/Biometrics for convenience.
- **Server Authentication**: Username/Password.
- **Multi-Tier Recovery Flow**:
  1. **Level 1 (New Device)**: Log in with existing credentials to sync data to a fresh app instance.
  2. **Level 2 (Lost Login Password)**: Use an **Admin Recovery Token** (generated during server setup) or a secondary email-based reset if the server is configured to send mail.
  3. **Level 3 (Worst Case - Server Access)**: Since you own the server, a CLI tool or direct access to the database (e.g., `sqlite` or `json` files) allows you to extract keys in plain text or reset user permissions manually. This ensures that as long as you have the server files, you never "lose" access.
- **Server Safety**: The server acts as a "Vault Manager". For maximum recovery, data can be stored unencrypted on the server's disk (protected by OS permissions) or encrypted with a key the server manages.

---

### 5. Multiplatform Client (Compose Multiplatform)
- **Shared Code**: Logic for API calls, data parsing, and caching.
- **Platform Specifics**: OS-level notifications or clipboard management.
- **UI**: A clean, modern list view with search and "copy to clipboard" buttons.

---

### 6. Deployment & Portability
- **Containerization**: Use **Docker** for one-click deployment.
- **Portability**: Data can be exported/imported via JSON, making it easy to ditch one server and move to another.
- **Service Discovery**: The client can be configured with multiple "seed" servers; once connected, it can discover other peers in the mesh.

## Verification Plan

### Manual Verification
- Deploy two server instances locally.
- Use a Desktop client to add a password to Server A.
- Shut down Server A.
- Open a Mobile/Web client (pointing to Server B) and verify the password appears (after sync).
- Simulate "Master Key Loss" by clearing client storage and re-authenticating to recover data.
