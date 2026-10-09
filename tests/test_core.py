import sys, tempfile, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'engine'))
from core import youtube_url,clip_range,framing,normalize_cues,caption_files,probe,export

class EngineTests(unittest.TestCase):
    def test_url_is_video_only(self):
        self.assertEqual(youtube_url('https://youtu.be/abcdefghijk?t=3'),'https://www.youtube.com/watch?v=abcdefghijk')
        for url in ('http://youtube.com/watch?v=abcdefghijk','https://youtube.com.evil.test/watch?v=abcdefghijk','https://localhost/watch?v=abcdefghijk','https://www.youtube.com/playlist?list=abc','https://user@youtube.com/watch?v=abcdefghijk'):
            with self.assertRaises(ValueError): youtube_url(url)
    def test_range_validation(self):
        self.assertEqual(clip_range({'start':1,'end':3},4),(1,3))
        for data in ({'start':-1,'end':3},{'start':3,'end':2},{'start':0,'end':181},{'start':float('nan'),'end':2}):
            with self.assertRaises(ValueError): clip_range(data,200)
    def test_no_upscale_portrait(self):
        for mode in ('crop','fit'):
            w,h,_=framing({'width':360,'height':640},3840,mode)
            self.assertEqual((w,h),(360,640))
            self.assertEqual(w*16,h*9)
    def test_caption_validation_and_escaping(self):
        with self.assertRaises(ValueError): normalize_cues([{'start':0,'end':2,'text':'a'},{'start':1,'end':3,'text':'b'}],4)
        with tempfile.TemporaryDirectory() as tmp:
            folder=Path(tmp)
            caption_files(folder,[{'start':0,'end':1,'text':r'Hello {\pos(1,1)} world'}],360,640)
            ass=(folder/'captions.ass').read_text()
            self.assertNotIn(r'{\pos',ass)
            self.assertIn('00:00:00,000 --> 00:00:01,000',(folder/'captions.srt').read_text())
    def test_real_export(self):
        source=Path(__file__).resolve().parent/'fixtures'/'source.mp4'
        if not source.exists(): self.skipTest('Provide generated fixture to run integration test')
        meta=probe(source)
        with tempfile.TemporaryDirectory() as tmp:
            folder=Path(tmp)
            for mode in ('crop','fit'):
                result=export(source,meta,{'start':1,'end':3,'framing':mode,'resolution':1920,'cues':[{'start':.1,'end':1.5,'text':'A clear caption'}]},folder)
                rendered=probe(folder/'clip.mp4')
                self.assertEqual(rendered['width']*16,rendered['height']*9)
                self.assertAlmostEqual(rendered['duration'],2,delta=.1)
                self.assertEqual(rendered['fps'],meta['fps'])
                self.assertEqual(result['captions'],1)
                self.assertTrue(rendered['audio'])

if __name__=='__main__': unittest.main()
