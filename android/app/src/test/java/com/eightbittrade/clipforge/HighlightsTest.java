package com.eightbittrade.clipforge;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class HighlightsTest {
 @Test public void replayPeaksProduceThreeDistinctClips(){List<Highlights.Pick> p=Highlights.recommend(300,Arrays.asList(new Highlights.Peak(40,.9),new Highlights.Peak(42,.8),new Highlights.Peak(140,.7),new Highlights.Peak(250,.6)),Collections.emptyList());assertEquals(3,p.size());assertTrue(p.get(0).reason.startsWith("YouTube"));for(int i=0;i<p.size();i++)for(int j=i+1;j<p.size();j++)assertTrue(Highlights.overlap(p.get(i),p.get(j))<=.25);}
 @Test public void unavailableHeatmapIsNeverPresentedAsReplayData(){for(Highlights.Pick p:Highlights.recommend(200,Collections.emptyList(),Collections.emptyList()))assertFalse(p.reason.contains("YouTube"));assertEquals(3,Highlights.recommend(200,Collections.emptyList(),Collections.emptyList()).size());}
 @Test public void shortVideoHasThreeBoundedSamplesWhenPossible(){List<Highlights.Pick> p=Highlights.recommend(45,Collections.singletonList(new Highlights.Peak(20,1)),Collections.emptyList());assertEquals(3,p.size());for(Highlights.Pick q:p){assertTrue(q.start>=0);assertTrue(q.end<=45);assertTrue(q.end>q.start);}}
 @Test public void tinyVideoDoesNotInventThreeClips(){assertEquals(1,Highlights.recommend(8,Collections.emptyList(),Collections.emptyList()).size());}
 @Test public void transcriptRecommendationsExplainTheirBasis(){List<Word>w=new ArrayList<>();for(int i=0;i<180;i++)w.add(new Word(i%10==0?"Why":"this",i*1.2,i*1.2+.6,.95));List<Highlights.Pick>p=Highlights.recommend(240,Collections.emptyList(),w);assertEquals(3,p.size());assertTrue(p.get(0).reason.startsWith("Content"));}
 @Test public void invalidHeatmapDoesNotEscapeVideoBounds(){List<Highlights.Pick>p=Highlights.recommend(90,Arrays.asList(new Highlights.Peak(-1,1),new Highlights.Peak(200,1),new Highlights.Peak(30,Double.NaN)),Collections.emptyList());assertEquals(3,p.size());for(Highlights.Pick q:p){assertTrue(q.start>=0);assertTrue(q.end<=90);}}
}
