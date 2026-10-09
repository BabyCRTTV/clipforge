import sys,os,tempfile,threading,json,time,unittest,urllib.request,urllib.error
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'engine'))

class HTTPTests(unittest.TestCase):
    def test_upload_export_download_and_origin(self):
        source=Path(__file__).resolve().parent/'fixtures'/'source.mp4'
        if not source.exists(): self.skipTest('Generated source fixture unavailable')
        with tempfile.TemporaryDirectory() as folder:
            os.environ['CLIPFORGE_DATA']=folder
            import server
            http=server.ThreadingHTTPServer(('127.0.0.1',0),server.Handler)
            server.PORT=http.server_address[1]
            threading.Thread(target=http.serve_forever,daemon=True).start()
            threading.Thread(target=server.worker,daemon=True).start()
            opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
            base='http://127.0.0.1:'+str(server.PORT)
            def get(path):
                with opener.open(base+path) as r: return r.read()
            try:
                token=json.loads(get('/api/session'))['token']
                req=urllib.request.Request(base+'/api/upload',data=source.read_bytes(),headers={'X-ClipForge-Token':token,'X-Filename':'source-test.mp4'},method='POST')
                with opener.open(req) as response: v=json.load(response)['video']
                req=urllib.request.Request(base+'/api/export',data=json.dumps({'video_id':v['id'],'start':1,'end':3,'resolution':1920,'cues':[{'start':0,'end':1.5,'text':'ClipForge test export'}]}).encode(),headers={'X-ClipForge-Token':token,'Content-Type':'application/json'},method='POST')
                with opener.open(req) as response: job=json.load(response)['job']
                for _ in range(100):
                    data=json.loads(get('/api/jobs/'+job))
                    if data['state'] in ('done','error'): break
                    time.sleep(.1)
                self.assertEqual(data['state'],'done',data)
                out=data['result']; self.assertGreater(len(get(out['video_url'])),1000); self.assertIn(b'ClipForge test export',get(out['srt_url']))
                req=urllib.request.Request(base+out['video_url'],headers={'Range':'bytes=0-99'})
                with opener.open(req) as r:
                    self.assertEqual(r.status,206);self.assertEqual(len(r.read()),100)
                with self.assertRaises(urllib.error.HTTPError) as caught:
                    opener.open(urllib.request.Request(base+'/api/import',data=b'{}',headers={'Origin':'https://evil.test'},method='POST'))
                self.assertEqual(caught.exception.code,403)
            finally: http.shutdown();http.server_close()

if __name__=='__main__': unittest.main()
