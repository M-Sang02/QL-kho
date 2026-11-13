# Android first-release specification

## Confirmed scope

- Android is the first platform. Web is planned after Android is stable and must use the same Firebase data.
- The first Android release must provide working product, stock-in, stock-out, order, and payment workflows. Do not expose placeholder actions for deferred capabilities.
- Google sign-in and warehouse membership/permissions are required from the first release.
- Firebase is the selected cloud backend. Room is the local source for offline use; a device that has signed in and downloaded data can reopen and work offline.
- Persist local changes across app restarts and app updates. Queue cloud synchronization while offline.

## Core inventory behavior

- A product requires a name and starts with a `Cái` branch. Search is case-insensitive.
- Each stock-in creates a distinct batch and records its purchase cost. Employee stock-ins remain pending approval; the owner can approve/reject, and the agreed automatic approval deadline is 10 minutes from device creation time.
- Stock-outs default to FIFO, with negative stock allowed. Each movement records its source transaction and before/after quantity.
- Units and batches remain separate; weight is normalized to kilograms internally. Defective/returned stock must not be included in sellable stock.

## Orders and payments

- A sale can contain multiple product/unit lines and records quantities and actual prices.
- A sale may be paid later or in installments. Stock decreases when goods are issued; revenue is counted when the order is fully paid and confirmed.
- A completed order can be edited; preserve before/after values and the editor/time. Quantity differences adjust stock.
- A completed order may be cancelled. Preserve its history and reverse stock; record cancellation separately from the employee's confirmation of the refund.
- Cancelling a partially paid order refunds the collected amount and records that refund separately.

## Membership and authorization

- Google account identity is required; warehouse code is for requesting membership, not a substitute for Google authentication.
- A join request not handled by the owner enters read-only access after 24 hours and employee operational permissions after a further 24 hours. Notify the owner immediately; a denial removes the member from the active group.
- Employee permissions never include account administration or owner-role assignment.
- Ownership recovery, membership-code rotation, permission revocation, and audit history must be server-authorized; client-only checks are insufficient.

## Setup and staged delivery

- `google-services.json`, an Android OAuth client configuration, Firebase Authentication's Google provider, and Firestore security rules are required before live sign-in/cloud sync can be exercised.
- Do not silently fall back to unauthenticated cloud access when Firebase is not configured.
- The initial implementation may be delivered in working slices, but every exposed control must work. Unimplemented return valuation, advanced reporting, notifications, and Web UI remain explicitly deferred rather than simulated.
- Current code keeps Room as the offline source, uses Firebase Authentication with Credential Manager, syncs warehouse records through immutable Firestore revisions, and supports owner-created share codes with owner/member rules. Firebase project configuration is still supplied by the app owner; employee approval timers, granular permissions, and per-member revocation remain deferred.
