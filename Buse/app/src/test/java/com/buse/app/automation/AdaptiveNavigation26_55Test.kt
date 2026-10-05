package com.buse.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class AdaptiveNavigation26_55Test {
    private fun rect(l: Int, t: Int, r: Int, b: Int) = Rect().apply { left=l; top=t; right=r; bottom=b }
    private fun n(label: String?, l: Int, t: Int, r: Int, b: Int) = NodeSnapshot(label,null,null,"android.widget.ImageButton",true,true,rect(l,t,r,b))
    private fun tablet(right: Boolean=false, width: Int=960, height: Int=1536, scale: Int=1): List<NodeSnapshot> {
        val x=if(right) width-63 else 63
        return listOf(n(null,0,0,width*scale,height*scale)) +
            listOf("Anasayfa","Ara","Grok","Bildirimler, 1 okunmamış","Mesajlar").mapIndexed { i,label ->
                n(label,(x-20)*scale,(537+i*96)*scale,(x+20)*scale,(577+i*96)*scale)
            }
    }
    @Test fun imageLayoutLeftRailSearchWorks() {
        assertEquals(2,DiscoverySearchSelector.searchTab(tablet()))
        assertEquals(AdaptiveNavigationEvidence.Edge.LEFT,AdaptiveNavigationEvidence.rail(tablet(),rect(0,0,960,1536))?.edge)
    }
    @Test fun mirroredRightRailSearchWorks() {
        assertEquals(2,DiscoverySearchSelector.searchTab(tablet(right=true)))
        assertEquals(AdaptiveNavigationEvidence.Edge.RIGHT,AdaptiveNavigationEvidence.rail(tablet(true),rect(0,0,960,1536))?.edge)
    }
    @Test fun scaledTabletLayoutsWork() {
        for(scale in 1..4) assertEquals(2,DiscoverySearchSelector.searchTab(tablet(scale=scale)))
    }
    @Test fun landscapeRailDoesNotRequireBottomSearch() {
        val nodes=tablet(width=1920,height=1200)
        assertEquals(2,DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun leftGestureGutterStaysInContent() {
        val nodes=tablet()
        val content=AdaptiveNavigationEvidence.contentViewport(nodes,rect(0,0,960,1536))
        assertEquals(126,content.left)
        for(attempt in 0..3) assertTrue(content.left+(content.right-content.left)*DiscoveryScrollGesturePolicy.xRatio(attempt)>129)
        assertEquals(960,content.right)
    }
    @Test fun rightGestureViewportExcludesRail() {
        val content=AdaptiveNavigationEvidence.contentViewport(tablet(true),rect(0,0,960,1536))
        assertEquals(0,content.left)
        assertEquals(834,content.right)
    }
    @Test fun bottomNavigationIsExcludedFromContent() {
        val nodes=listOf(n(null,0,0,1080,2400))+listOf("Home","Search","Grok","Notifications","Messages").mapIndexed { i,label ->n(label,i*210,2260,i*210+90,2350) }
        assertEquals(2,DiscoverySearchSelector.searchTab(nodes))
        assertEquals(2260,AdaptiveNavigationEvidence.contentViewport(nodes,rect(0,0,1080,2400)).bottom)
    }
    @Test fun missingSearchLabelUsesOnlyProvenRailSlot() {
        val nodes=tablet().toMutableList();nodes[2]=nodes[2].copy(text=null)
        assertEquals(2,DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun ambiguousBlankRailSlotsAreNotGuessed() {
        val nodes=tablet().toMutableList();nodes[2]=nodes[2].copy(text=null)
        nodes.add(n(null,43,605,83,630))
        assertNull(DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun hiddenSearchDoesNotCauseAnotherIconClick() {
        val nodes=tablet().toMutableList();nodes[2]=nodes[2].copy(visible=false)
        assertNull(DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun disabledSearchIsNotPressed() {
        val nodes=tablet().toMutableList();nodes[2]=nodes[2].copy(enabled=false)
        assertNull(DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun editableSearchIsNotNavigation() {
        val nodes=tablet().toMutableList();nodes[2]=nodes[2].copy(editable=true)
        assertNull(DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun profileSearchDoesNotReplaceRailSearch() {
        val nodes=tablet()+n("Ara",700,60,750,110)
        assertEquals(2,DiscoverySearchSelector.searchTab(nodes))
    }
    @Test fun unprovenSideLabelsDoNotTrimContent() {
        val nodes=listOf(n(null,0,0,960,1536),n("Home",43,537,83,577),n("Ara",43,633,83,673))
        assertNull(AdaptiveNavigationEvidence.rail(nodes,rect(0,0,960,1536)))
        assertNull(DiscoverySearchSelector.searchTab(nodes))
        assertEquals(0,AdaptiveNavigationEvidence.contentViewport(nodes,rect(0,0,960,1536)).left)
    }
    @Test fun postTextContainingNavigationWordsIsNotAnAnchor() {
        val nodes=listOf(n(null,0,0,960,1536),n("Home today",43,537,83,577),n("Ara beni",43,633,83,673),n("Messages from friends",43,729,83,769))
        assertNull(AdaptiveNavigationEvidence.rail(nodes,rect(0,0,960,1536)))
    }
    @Test fun offsetWindowUsesItsOwnEdges() {
        val nodes=tablet().map { it.copy(bounds=rect(it.bounds.left+300,it.bounds.top+80,it.bounds.right+300,it.bounds.bottom+80)) }
        assertEquals(2,DiscoverySearchSelector.searchTab(nodes))
        assertEquals(426,AdaptiveNavigationEvidence.contentViewport(nodes,rect(300,80,1260,1616)).left)
    }
    @Test fun unknownGeometryKeepsViewportAndDoesNotInventSearch() {
        val nodes=listOf(n(null,0,0,960,1536),n(null,43,633,83,673))
        assertNull(DiscoverySearchSelector.searchTab(nodes))
        assertEquals(960,AdaptiveNavigationEvidence.contentViewport(nodes,rect(0,0,960,1536)).right)
    }
    @Test fun duplicateSemanticNodesDoNotInventAmbiguousSearch() {
        val nodes=tablet()+tablet()[2]
        assertNull(AdaptiveNavigationEvidence.searchIndex(nodes))
    }
}
