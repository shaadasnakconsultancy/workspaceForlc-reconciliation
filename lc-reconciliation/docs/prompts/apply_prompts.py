"""
Apply the prompt files in this folder to a LetterOfCreditDB database.

The application reads its prompts from the prompt_templates table, not from disk, so these files
are the version-controlled master copy and this script pushes them into a database.

For each document type it deactivates the current active prompt and inserts a NEW version, so the
previous prompt stays in the table and can be re-activated from the Prompt Templates page if the
change needs to be rolled back.

Usage:
    python apply_prompts.py --server 139.59.4.149,1433 --user sa --password *** [--dry-run]

Requires sqlcmd on PATH (ships with SQL Server client tools).
"""

import argparse
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))

# document type code -> (prompt file, schema file or None to keep the existing schema)
TARGETS = {
    "MASTER_LC": ("MASTER_LC_prompt.txt", None),
    "INVOICE": ("INVOICE_prompt.txt", "INVOICE_response_schema.json"),
}


def sql_literal(text):
    """Wrap text as an N'' literal, doubling embedded single quotes."""
    return "N'" + text.replace("'", "''") + "'"


def build_script(args):
    statements = ["SET NOCOUNT ON;", "SET XACT_ABORT ON;", "BEGIN TRANSACTION;"]

    for type_code, (prompt_file, schema_file) in TARGETS.items():
        with open(os.path.join(HERE, prompt_file), encoding="utf-8") as fh:
            prompt_text = fh.read()

        if schema_file:
            with open(os.path.join(HERE, schema_file), encoding="utf-8") as fh:
                schema_expr = sql_literal(fh.read().strip())
        else:
            # keep whatever schema the current active prompt uses
            schema_expr = (
                "(SELECT TOP 1 p.response_schema FROM prompt_templates p "
                "JOIN document_types d ON d.id = p.document_type_id "
                "WHERE d.type_code = '{0}' AND p.is_active = 1 ORDER BY p.version DESC)"
            ).format(type_code)

        statements.append(
            """
DECLARE @dt_{code} BIGINT = (SELECT id FROM document_types WHERE type_code = '{code}');
IF @dt_{code} IS NULL
    RAISERROR('Document type {code} not found', 16, 1);

DECLARE @schema_{code} NVARCHAR(MAX) = {schema};
DECLARE @ver_{code} INT = ISNULL((SELECT MAX(version) FROM prompt_templates WHERE document_type_id = @dt_{code}), 0) + 1;

UPDATE prompt_templates SET is_active = 0 WHERE document_type_id = @dt_{code};

INSERT INTO prompt_templates (document_type_id, prompt_name, prompt_text, response_schema, is_active, version)
VALUES (@dt_{code}, '{code} Prompt v' + CAST(@ver_{code} AS VARCHAR(10)), {prompt}, @schema_{code}, 1, @ver_{code});

PRINT '{code}: inserted version ' + CAST(@ver_{code} AS VARCHAR(10));
""".format(code=type_code, prompt=sql_literal(prompt_text), schema=schema_expr)
        )

    statements.append("COMMIT TRANSACTION;" if not args.dry_run else "ROLLBACK TRANSACTION;")
    statements.append(
        "SELECT d.type_code, p.version, p.is_active, LEN(p.prompt_text) AS prompt_len "
        "FROM prompt_templates p JOIN document_types d ON d.id = p.document_type_id "
        "WHERE d.type_code IN ('MASTER_LC','INVOICE') ORDER BY d.type_code, p.version;"
    )
    return "\n".join(statements)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--server", help="e.g. 139.59.4.149,1433 (not needed with --sql-out)")
    ap.add_argument("--database", default="LetterOfCreditDB")
    ap.add_argument("--user")
    ap.add_argument("--password")
    ap.add_argument("--dry-run", action="store_true",
                    help="run everything inside a transaction that is rolled back")
    ap.add_argument("--sql-out", metavar="FILE",
                    help="write the SQL to FILE instead of running it (for running on a server "
                         "by hand, e.g. in SSMS)")
    args = ap.parse_args()

    script = build_script(args)

    if args.sql_out:
        with open(args.sql_out, "w", encoding="utf-8") as fh:
            fh.write(script)
        print("SQL written to:", args.sql_out)
        return 0

    if not (args.server and args.user and args.password):
        ap.error("--server, --user and --password are required unless --sql-out is used")

    path = None
    try:
        with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False, encoding="utf-8") as fh:
            fh.write(script)
            path = fh.name
        result = subprocess.run(
            ["sqlcmd", "-S", args.server, "-d", args.database, "-U", args.user,
             "-P", args.password, "-C", "-b", "-i", path],
            capture_output=True, text=True)
        sys.stdout.write(result.stdout)
        if result.stderr.strip():
            sys.stderr.write(result.stderr)
        return result.returncode
    finally:
        if path and os.path.exists(path):
            os.remove(path)


if __name__ == "__main__":
    sys.exit(main())
