# Invoice reconciliation prompts

The application reads its prompts from the `prompt_templates` table, **not** from this folder.
These files are the version-controlled master copy. `PromptLoader.java` is disabled (its call in
`AppContextListener` is commented out), so nothing loads them automatically.

| File | Applies to |
|---|---|
| `MASTER_LC_prompt.txt` | Master LC parameter extraction (builds the ComplianceMasterList) |
| `INVOICE_prompt.txt` | Invoice reconciliation (all matching rules) |
| `INVOICE_response_schema.json` | Response schema for the invoice prompt |
| `apply_prompts.py` | Pushes the files above into a database |

## Applying to a database

```bash
python apply_prompts.py --server <host,port> --user <user> --password <pw> --dry-run
python apply_prompts.py --server <host,port> --user <user> --password <pw>
```

It deactivates the current prompt and inserts a **new version**, so the previous one stays in the
table. To roll back, open Master Data > Prompt Templates and re-activate the older version.

The same text can be pasted into the Prompt Templates page by hand if running a script against
PROD is not allowed.

## Changes in this version (2026-09-22)

Scope: invoice reconciliation only. No other document type's prompt was touched.

### Master LC prompt

- `PortOfLoading` now reads **clause 44E** (it used to be taken from the invoice and never checked).
- `IncoTerms` keeps the **full trade terms statement** instead of being cut down to `FOB`.
- `LCDateOfIssue` keeps the LC's original date format, so the invoice prompt can interpret it.
- `LCIssuingBankBinNo` must be the **issuing bank's** BIN, explicitly not the importer's BIN/TIN.
- Added: `EngineCC`, `HSCode`, `ProformaInvoiceNoAndDate` (all clause 45A).
- Removed: `FinalDestination` and `IssuingBankBINNumber` (duplicates).
- Replaced: `ProformaInvoiceNo` (number only) by `ProformaInvoiceNoAndDate` (full sentence).

### Invoice prompt

- Blanket loose matching is gone. The default is now **STRICT MATCH**, which allows only two
  exemptions: letter case, and abbreviations with their punctuation (LTD/LIMITED, PVT/PRIVATE …).
  Scan artefacts (line breaks, repeated spaces) are still normalised.
- `ExporterShipperAddress` is **always Complied** (also enforced in Java, see below).
- `PortOfLoading` (44E) and `PortOfDischarge` (44F) apply the ANY SEA PORT rule: a generic LC value
  takes the port name from the invoice and yields e.g. `PIPAVAV SEA PORT OF INDIA`; a named LC port
  must match. An invoice with no port name is Not Complied.
- `LCDateOfIssue` compares **calendar dates**, so `260809` and `09.08.2026` match.
- `LCIssuingBankBinNo` requires an exact BIN, has no fallback to other bank text, and is Not
  Complied when unreadable.
- `IncoTerms` compares term + port + incoterms year after substituting `ANY` with the invoice port
  of loading. Different wording with the same meaning is Complied **with a note recorded in the
  reason column**.
- `ProductionMonth` and `EngineCC` check **every lot** on the invoice: all lots must agree with
  each other and with clause 45A.
- `HSCode` compares the code list on the invoice's first page against clause 45A. Any label
  (`HS CODE`, `HSN CODE`, `IMPORTERS HS CODE`) is accepted; order and separators are ignored.
- `ProformaInvoiceNoAndDate` matches the full sentence anywhere on the invoice.
- A `Complied` row may now carry a reason, used only where a rule says to record a note.

### Schema

`invoice_summary` no longer contains `FinalDestination`. The copy inside `PromptLoader.java` was
updated to match, even though that class is dormant.

### Java

`ReconciliationJobRunner.forceCompliedParameters()` forces `ExporterShipperAddress` to Complied
after the model replies, so the outcome does not depend on the model obeying the prompt.

### Master data

`document_types.page_limit` for `INVOICE` was raised from **1 to 3** so that lots continuing onto
later pages are read. This is a database value, not code: **set it in PROD as well**, via
Master Data > Document Types. It increases OCR cost per invoice.

## Still unverified

These prompts have not yet been run against a real LC and invoice. The HS code, Engine CC,
Production Month, port and proforma rules need one end-to-end job before PROD.
