"""Build a test-only player-processing fixture from a public upstream regression case.
The downloaded YouTube script is not committed or bundled in the release APK.
"""
from pathlib import Path
import hashlib,json,sys,urllib.request
root=Path(__file__).resolve().parent
sys.path[:0]=[str(root/'app/src/main/assets/downloader/yt_dlp_ejs-0.8.0-py3-none-any.whl')]
from yt_dlp_ejs.yt.solver import core,lib
assets=root/'app/src/androidTest/assets';assets.mkdir(parents=True,exist_ok=True)
url='https://www.youtube.com/s/player/74edf1a3/player_ias.vflset/en_US/base.js'
player=urllib.request.urlopen(url,timeout=60).read().decode()
if hashlib.sha256(player.encode()).hexdigest()!='ed9a4ea0646332a6b6c973ebe5a915c62c43db89bb2bb1a31eba07d906ef8ff7':
    raise SystemExit('Regression player checksum changed; test fixture build stopped.')
# Test vectors maintained by yt-dlp's test/test_jsc/test_ejs_integration.py.
sig='NJAJEij0EwRgIhAI0KExTgjfPk-MPM9MAdzyyPRt=BM8-XO5tm5hlMCSVpAiEAv7eP3CURqZNSPow8BXXAoazVoXgeMP7gH9BdylHCwgw=gwzz'
expected_sig='NJAJEij0EwRgIhAI0KExTgjfPk-MPM9MAdzyyPRt=BM8-XO5tm5hzMCSVpAiEAv7eP3CURqZNSPow8BXXAoazVoXgeMP7gH9BdylHCwgw=gwzl'
request={'type':'player','player':player,'requests':[{'type':'n','challenges':['IlLiA21ny7gqA2m4p37']},{'type':'sig','challenges':[sig]}],'output_preprocessed':False}
(assets/'youtube-player-check.js').write_text(lib()+'\nObject.assign(globalThis,lib);\n'+core()+'\nconsole.log(JSON.stringify(jsc('+json.dumps(request)+')));',encoding='utf-8')
(assets/'youtube-player-expected.json').write_text(json.dumps({'n':'9nRTxrbM1f0yHg','sig':expected_sig}))
print('Prepared test-only YouTube player',len(player),'bytes; sha256',hashlib.sha256(player.encode()).hexdigest())
