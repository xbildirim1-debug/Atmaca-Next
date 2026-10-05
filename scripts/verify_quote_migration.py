"""Execute the actual 5->6 migration SQL against both historical v5 layouts."""
import hashlib
from pathlib import Path
import re
import sqlite3
import uuid

source = Path("app/src/main/java/com/atmacanext/app/data/local/AtmacaDatabase.kt").read_text()
migration = source.split("internal val MIGRATION_5_6", 1)[1].split("fun create", 1)[0]
statements = re.findall(r'db\.execSQL\("([^"\n]+)"', migration)
assert len(statements) == 8, "Inspect changed migration SQL before updating this verifier"


def name_uuid(seed):
    return str(uuid.UUID(bytes=hashlib.md5(seed.encode()).digest(), version=3))


for default in ("STANDARD", "'STANDARD'"):
    db = sqlite3.connect(":memory:")
    db.execute("CREATE TABLE scheduled_tasks (id TEXT PRIMARY KEY, progress INTEGER NOT NULL)")
    db.execute("INSERT INTO scheduled_tasks VALUES ('task', 2)")
    db.execute("CREATE TABLE target_accounts (id TEXT NOT NULL PRIMARY KEY, ownerAccountId TEXT NOT NULL, handle TEXT NOT NULL, active INTEGER NOT NULL, updatedAt INTEGER NOT NULL, kind TEXT NOT NULL DEFAULT " + default + ")")
    quote_id = name_uuid("owner:QUOTE:hedef")
    standard_id = name_uuid("owner:STANDARD:hedef")
    rows = [(quote_id, "owner", "hedef", 1, 123, "STANDARD"),
            (standard_id, "owner", "hedef", 1, 124, "STANDARD"),
            ("custom", "other-owner", "other", 0, 125, "QUOTE")]
    db.executemany("INSERT INTO target_accounts VALUES (?,?,?,?,?,?)", rows)
    with db:
        for sql in statements:
            if sql.startswith("UPDATE target_accounts"):
                for row_id, owner, handle in db.execute("SELECT id, ownerAccountId, handle FROM target_accounts WHERE kind = 'STANDARD'").fetchall():
                    if name_uuid(owner + ":QUOTE:" + handle) == row_id:
                        db.execute(sql, (row_id,))
            else:
                db.execute(sql)
    expected = [(quote_id, "owner", "hedef", 1, 123, "QUOTE"), rows[1], rows[2]]
    actual = db.execute("SELECT id,ownerAccountId,handle,active,updatedAt,kind FROM target_accounts").fetchall()
    assert sorted(actual) == sorted(expected), "Migration must preserve every target and repair only the proven quote row"
    info = {row[1]: row for row in db.execute("PRAGMA table_info(target_accounts)")}
    assert info["kind"][4] == "'STANDARD'"
    assert db.execute("SELECT id,progress,quoteTargets,quotePostedKeys,quotePendingKey FROM scheduled_tasks").fetchall() == [("task", 2, None, None, None)]
    assert not db.execute("SELECT name FROM sqlite_master WHERE name='target_accounts_v6'").fetchall()
    db.close()
print("Quote migration: PASS (both v5 defaults, targets and task progress preserved)")
