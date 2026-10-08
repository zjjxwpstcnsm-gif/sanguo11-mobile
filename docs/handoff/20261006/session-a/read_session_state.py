"""Bounded retry of an in-place session receipt write; never restart its producer."""
import json,time
def read_session_state(path):
 for attempt in range(30):
  try:return json.loads(path.read_text())
  except json.JSONDecodeError:
   if attempt==29:raise
   time.sleep(.1)
