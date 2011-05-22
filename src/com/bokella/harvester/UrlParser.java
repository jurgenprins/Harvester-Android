package com.bokella.harvester;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.util.Log;

public class UrlParser {
	LinkCollection linkCollection 	= null;
	HtmlCleaner cleaner = null;
	Pattern pCRLF 		= Pattern.compile("[\\n\\r\\t\\s]+");
	Pattern pBLOCK 		= Pattern.compile("\\{(.*?)\\}");
	Pattern pPAREN 		= Pattern.compile("\\((.*?)\\)");
	Pattern pTAG 		= Pattern.compile("<(.*?)>");
	Pattern pJUNK		= Pattern.compile("[\\{\\(\\}\\)]+");

	/*
	Log Log = new Log();
	public class Log {
		public void i(String x, String y) { 		System.out.println(y); 		}
		public void e(String x, String y) { 		System.out.println(y); 		}
	}
	*/
	
	public UrlParser() {
		cleaner = new HtmlCleaner();
		
		CleanerProperties props = cleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);
	}

	public List<WebItem> getItems(String url) throws Exception {
		URL urlBase = new URL(url);
		
		Log.i(Harvester.TAG, "Cleaning " + url);
		TagNode node = cleaner.clean((InputStream) urlBase.getContent());
		Log.i(Harvester.TAG, "Done cleaning " + url);
		
		// Count number of (same) images
		Map<Integer, Integer> imageHashes	= new HashMap<Integer, Integer>();
		Integer imgOccurs					= 0;
		String linkHref 					= null;
		int	imgHash							= 0;
		
		TagNode[] tags = node.getElementsByName("img", true);
		Log.i(Harvester.TAG, "Registering " + tags.length + " images");
		for (TagNode tag : tags) {
			if ((linkHref = tag.getAttributeByName("src")) != null) {
				if ((imgOccurs = imageHashes.get(imgHash = linkHref.hashCode())) != null) { 
					imageHashes.put(imgHash, imgOccurs + 1);
				} else {
					imageHashes.put(imgHash, 1);
				}
			}
		}
		Log.i(Harvester.TAG, "Done registering images");
	
		linkCollection	= new LinkCollection(urlBase);
		tags 			= node.getElementsByName("a", true);
		
		Log.i(Harvester.TAG, "Registering " + tags.length + " links");
		for (TagNode tag : tags) {
			if ((linkHref = tag.getAttributeByName("href")) != null) {
				linkHref = this.absolutize(urlBase, linkHref);
				
				// if link is already in page, skip the duplicate, we are only
				// interested in uniquely linked items
				if (linkCollection.contains(linkHref)) {
					continue;
				}
				
				linkCollection.add(linkHref, tag);
			}
		}
		Log.i(Harvester.TAG, "Done registering links");
		
		List<WebItem>	webItems	= new ArrayList<WebItem>();
		List<String> 	links 		= linkCollection.getLinksByHighestScoring();
		
		TagNode tag 		= null;
		WebItem webItem		= null;
		String linkTitle 	= null;
		String imgSrc 		= null;
		String txtSummary	= null;
		int maxLevelsScan	= 0;
		
		Log.i(Harvester.TAG, "Parsing " + links.size() + " best scoring links");
		for (int i = 0; i < links.size(); i++) {
			webItem		= new WebItem();
			webItem.setUrl(links.get(i));
			
			tag = linkCollection.getTag(webItem.getUrl());
			if (tag == null) {
				Log.e(Harvester.TAG, "  no tag registered for " + webItem.getUrl());
				continue;
			}
			
			if ((linkTitle = tag.getText().toString()).length() > 0) {
				webItem.setTitle(this.strip(tag.getText().toString()));
			}

			if ((linkTitle = tag.getAttributeByName("title")) != null) {
				webItem.setTitle(this.strip(linkTitle));
			}

			maxLevelsScan 	= 5;
			while (true) {
				if (maxLevelsScan-- > 0) {
					if (webItem.getThumbUrl() == null) {
						Object[] imgTags = tag.evaluateXPath("//img");
						if (imgTags != null) {
							for (Object imgTag : imgTags) {
								if ((imgSrc = ((TagNode) imgTag).getAttributeByName("src")) != null) {
									// Find out if this image is displayed multiple times.. then it does not represent this item uniquely..
									if ((imgOccurs = imageHashes.get(imgSrc.hashCode())) != null) {
										if (imgOccurs > 3) {
											continue;
										}
									}
									
									webItem.setThumbUrl(this.absolutize(urlBase, imgSrc));
									
									if ((webItem.getTitle() == null) &&
										((linkTitle = ((TagNode) imgTag).getAttributeByName("alt")) != null)) {
											webItem.setTitle(this.strip(linkTitle));
									}
								}
							}
						}
					}

					if ((webItem.getSummary() == null) &&
						((txtSummary = tag.getText().toString()).length() > 0)) {
						txtSummary = this.strip(txtSummary);
						if ((linkTitle = webItem.getTitle()) != null) {
							txtSummary = txtSummary.replace(linkTitle, "");
						}
						if (txtSummary.length() > 0) {
							webItem.setSummary(txtSummary);
						}
					}
				}
			
				// If at top of tag hierarchy, stop search
				if ((tag = tag.getParent()) == null) {
					break;
				}
			}
			
			if (webItem.getTitle() == null) {
				URI uri = new URI(webItem.getUrl());
				webItem.setTitle(uri.getPath());
			}

			webItem.setStatus("");
			
			webItems.add(webItem);
		}
		
		// Cleaning up summaries that have been composed to greedy out of its surrounding links/items
		Log.i(Harvester.TAG, "Cleaning up " + webItems.size() + " parsed items");
		int j= 0;
		for (int i = 0; i < webItems.size(); i++) {
			if ((txtSummary = webItems.get(i).getSummary()) != null) {
				for (j = 0; j < webItems.size(); j++) {
					if ((linkTitle = webItems.get(j).getTitle()) != null) {
						webItems.get(i).setSummary(txtSummary = txtSummary.replace(linkTitle, ""));
					}
				}
				webItems.get(i).setSummary(org.apache.commons.lang.StringUtils.abbreviate(webItems.get(i).getSummary(), 255));
			}
		}
		Log.i(Harvester.TAG, "Done parsing best scoring links");
		
		return webItems;
	}

	public String absolutize(URL base, String url) {
		if (url != null) {
			if (!url.startsWith("http") && !url.startsWith("ftp")) {
				try {
					url = new URL(base, url).toString();
					int s = url.lastIndexOf("#");
					if (s > 5) {
						url = url.substring(0, s);
					}
				} catch (Exception e) {
				}
			}
			return url;
		} else {
			return "";
		}
	}
	
	public String strip(String str) {
		return 
				pJUNK.matcher(
						pTAG.matcher(
								pBLOCK.matcher(
										pPAREN.matcher(
												pCRLF.matcher(org.apache.commons.lang.StringEscapeUtils.unescapeHtml(str)).replaceAll(" ")
										).replaceAll("")
								).replaceAll("")
						).replaceAll("")
				).replaceAll("").trim();
	}
}
