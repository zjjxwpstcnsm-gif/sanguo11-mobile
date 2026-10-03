#!/usr/bin/env python3
"""Pin the merchant integration's ordered commit, month and harvest-clear blocks."""
import argparse
import hashlib
import json
from pathlib import Path

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def inspect(exe):
    raw=exe.read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
    blocks=[]
    for name,start,end in [('ordered_experience_quote_credit',0x5cacb7,0x5cad20),('global_monthly_day_guard',0x590c67,0x590c79),('weather_before_city_monthly',0x590ca0,0x590cb7),('city_monthly_price_dispatch',0x590648,0x590657),('harvest_august_guard_and_clear',0x58f52b,0x58f555)]:
        data=raw[start-0x400000:end-0x400000];blocks.append(dict(name=name,start=hex(start),end_exclusive=hex(end),sha256=hashlib.sha256(data).hexdigest(),bytes_hex=data.hex()))
    return dict(schema=1,source_file='san11pk.exe',executable_sha256=EXE_SHA,blocks=blocks,
        verified=dict(maximum_before_xp_final_credit=24,harvest_month_city_observations=504,monthly_day_guard=36,world_memory_bytes=0x300000,rule_rng_unchanged=True),
        normal_commit_order=['UI quantity maximum uses pre-XP current politics','politics XP +5 capped3000 and ability refresh','quote uses post-XP current politics','signed food/gold transfer; gold credit clamps100000','merit/action/city-use and20AP'],
        monthly_order=['month controller only on current day1','ordinary weather controller before city/month dispatch','harvest bit2 cleared in August','price update before remaining per-city monthly loyalty work'],
        production=dict(save_version=35,legacy_versions='31-33 unchanged numeric/price mode and write33;34 keeps ability state and old price mode and writes34',quantity='positive food delta1..1000000 within native pre-XP stock maximum; no thousand rounding',price_state='per-city native byte rate plus saved harvest and processed-month clock',rng='existing saved Strategy SplitMix64 adapter; previews use no randomness'),
        limits=['Full native global RNG stream parity is unresolved; no new isolated merchant RNG is fabricated','Native whole weather/settings/loyalty/monthly controller is not emulated; bounded guard/clear/price/commit blocks only','Original facility notifications following harvest clear remain unexecuted','Project disaster/harvest generation and lifetimes remain uncalibrated; market consumes actual saved event state','Official/MOD active content and city/scenario mapping remain separate; no official-opening claim'])


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--exe',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
    if args.exe.parent.resolve()==args.output.resolve() or args.exe.parent.resolve() in args.output.resolve().parents:raise ValueError('Read-only installation')
    args.output.write_text(json.dumps(inspect(args.exe),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
