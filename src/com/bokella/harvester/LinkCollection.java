package com.bokella.harvester;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.htmlcleaner.TagNode;

public class LinkCollection {
	URL 							url					= null;
	Map<Integer, TagNode> 			linkTags			= null;
	Map<String, ArrayList<String>> 	linkGroups 			= null;
	Map<Integer, String> 			linkXPath			= null;
	Map<String, ArrayList<String>> 	linkGroupsWithID	= null;
	Map<Integer, String> 			linkXPathWithID		= null;
	
	public LinkCollection(URL url) {
		this.url 				= url;
		this.linkTags			= new HashMap<Integer, TagNode>();
		this.linkGroups 		= new HashMap<String, ArrayList<String>>();
		this.linkXPath			= new HashMap<Integer, String>();
		this.linkGroupsWithID	= new HashMap<String, ArrayList<String>>();
		this.linkXPathWithID	= new HashMap<Integer, String>();
	}

	public boolean contains(String url) {
		return (linkTags.containsKey(url.hashCode()));
	}
	
	public void add(String url, TagNode tag) {
		String[] xpaths = this.findXPaths(tag);
		
		if (!linkGroups.containsKey(xpaths[0])) {
			linkGroups.put(xpaths[0], new ArrayList<String>());
		}
		linkGroups.get(xpaths[0]).add(url);
		linkXPath.put(url.hashCode(), xpaths[0]);
		
		if (!linkGroupsWithID.containsKey(xpaths[1])) {
			linkGroupsWithID.put(xpaths[1], new ArrayList<String>());
		}
		linkGroupsWithID.get(xpaths[1]).add(url);
		linkXPathWithID.put(url.hashCode(), xpaths[1]);
		
		linkTags.put(url.hashCode(), tag);
	}
	
	public TagNode getTag(String url) {
		return linkTags.get(url.hashCode());
	}
	
	public ArrayList<String> getLinksByHighestScoring() {
		Map<String, String> itemGroupScore = new TreeMap<String, String>();
		
		for(Object itemGroupKey : linkGroups.keySet().toArray()) {
			itemGroupScore.put(String.format("%06d", this.getGroupScore((String)itemGroupKey)).concat("|").concat((String)itemGroupKey), (String)itemGroupKey);
		}
		
		Object[] scores = itemGroupScore.keySet().toArray();
		if (scores.length <= 0) {
			return null;
		}
		
		return linkGroups.get(itemGroupScore.get(scores[scores.length - 1]));
	}
	
	private int getGroupScore(String itemGroupKey) {
		ArrayList<String> 		links 				= linkGroups.get(itemGroupKey);
		int						score				= links.size();
		Map<String, Integer> 	xPathWithID			= new HashMap<String, Integer>();
		Integer 				xPathWithIDOccurs	= 0;
		String					xPathKey			= "";
		
		for (String url : links) {
			if ((xPathWithIDOccurs = xPathWithID.get(xPathKey = linkXPathWithID.get(url.hashCode()))) != null) { 
				xPathWithID.put(xPathKey, xPathWithIDOccurs + 1);
			} else {
				xPathWithID.put(xPathKey, 1);
			}
		}
		
		for (String url : links) {
			//System.out.println("SCORING: " + url + " " + itemGroupKey + "(" + score + ") / " + linkXPathWithID.get(url.hashCode()) + "(" + linkGroupsWithID.get(linkXPathWithID.get(url.hashCode())).size() + ") ");
			if ((xPathWithIDOccurs = xPathWithID.get(linkXPathWithID.get(url.hashCode()))) < (links.size() / 2)) { 
				if (linkGroupsWithID.get(linkXPathWithID.get(url.hashCode())).size() > 1) {
					//System.out.println("PENALTY: " + url + " occurs in group " + linkXPathWithID.get(url.hashCode()) + " (found " + xPathWithIDOccurs + " times) , score of " + score + " will be decreased");
					score -= 1;
				}
			}
			if (xPathWithIDOccurs == links.size()) {
				score += 1;
			}
		}
		
		//System.out.println ("Score OF " + itemGroupKey + " = " + score);
		return (score > 0) ? score : 0;
	}

	private String[] findXPaths(TagNode tag) {
		String[] xpaths = new String[2]; xpaths[0] = ""; xpaths[1] = "";
		String attrID			= "";
		while ((tag = tag.getParent()) != null) {
			xpaths[0] = xpaths[0].concat("/").concat(tag.getName());
			if ((attrID = tag.getAttributeByName("id")) != null) {
				xpaths[1] = xpaths[1].concat("/").concat(tag.getName()).concat("|").concat(attrID);
			} else {
				xpaths[1] = xpaths[1].concat("/").concat(tag.getName());
			}
		}
		return xpaths;
	}

}
