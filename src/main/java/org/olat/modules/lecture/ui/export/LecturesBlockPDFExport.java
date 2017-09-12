/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.lecture.ui.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.pdf.PdfDocument;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureBlockRollCall;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Initial date: 22 juin 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LecturesBlockPDFExport extends PdfDocument implements MediaResource {
	
	private static final OLog log = Tracing.createLoggerFor(LecturesBlockPDFExport.class);
	
	private String teacher;
	private int numOfLectures;
	private final Translator translator;
	private final LectureBlock lectureBlock;
	private final RepositoryEntry entry;
	
	public LecturesBlockPDFExport(LectureBlock lectureBlock, Translator translator)
			throws IOException {
		super(translator.getLocale());
		
		marginTopBottom = 62.0f;
		marginLeftRight = 62.0f;
		entry = lectureBlock.getEntry();
		this.translator = translator;
		this.lectureBlock = lectureBlock;
		
		numOfLectures = lectureBlock.getEffectiveLecturesNumber();
		if(numOfLectures <= 0) {
			numOfLectures = lectureBlock.getPlannedLecturesNumber();
		}
	}

	public String getTeacher() {
		return teacher;
	}

	public void setTeacher(String teacher) {
		this.teacher = teacher;
	}

	@Override
	public boolean acceptRanges() {
		return false;
	}

	@Override
	public String getContentType() {
		return "application/pdf";
	}

	@Override
	public Long getSize() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public Long getLastModified() {
		return null;
	}

	@Override
	public void prepare(HttpServletResponse hres) {
		try {
			Formatter formatter = Formatter.getInstance(translator.getLocale());
			String filename = lectureBlock.getTitle()
					+ "_" + formatter.formatDate(lectureBlock.getStartDate())
					+ "_" + formatter.formatTimeShort(lectureBlock.getStartDate())
					+ "-" + formatter.formatTimeShort(lectureBlock.getEndDate())
					+ ".pdf";
			hres.setHeader("Content-Disposition","attachment; filename*=UTF-8''" + StringHelper.urlEncodeUTF8(filename));			
			hres.setHeader("Content-Description",StringHelper.urlEncodeUTF8(filename));
			document.save(hres.getOutputStream());
		} catch (COSVisitorException | IOException e) {
			log.error("", e);
		}
	}

	@Override
	public void release() {
		try {
			close();
		} catch (IOException e) {
			log.error("", e);
		}
	}

	public void create(List<Identity> rows, List<LectureBlockRollCall> rollCalls)
    throws IOException, COSVisitorException, TransformerException {
	    	addPage();
	    	String lectureBlockTitle = lectureBlock.getTitle();
	    	String resourceTitle = entry.getDisplayname();
	    	addMetadata(lectureBlockTitle, resourceTitle, teacher);
	
	    	String title = resourceTitle + " - " + lectureBlockTitle;
	    	addParagraph(title, 16, true, width);
	
	    	Formatter formatter = Formatter.getInstance(translator.getLocale());
		String dates = translator.translate("pdf.table.dates", new String[] {
			formatter.formatDate(lectureBlock.getStartDate()),
			formatter.formatTimeShort(lectureBlock.getStartDate()),
			formatter.formatTimeShort(lectureBlock.getEndDate())
		});
	
	    	addParagraph(dates, 12, true, width);
	  	
	    	float cellMargin = 5.0f;
	    	float fontSize = 10.0f;
	    	
	    	String[] content = getRows(rows);
	    	boolean[][] absences = getRowAbsences(rows, rollCalls);
	    	String[] comment = getRowComment(rows, rollCalls);
	    	
	    	int numOfRows = content.length;
	    	for(int offset=0; offset<numOfRows; ) {
	    		offset += drawTable(content, absences, comment, offset, fontSize, cellMargin);
	    		closePage();
	        	if(offset<numOfRows) {
	        		addPage();
	        	}
	    	}
	    	
	    	addPageNumbers(); 
	}
		
	private String[] getRows(List<Identity> rows) {
		int numOfRows = rows.size();
	
	    	String[] content = new String[numOfRows];
	    	for(int i=0; i<numOfRows; i++) {
	    		Identity row = rows.get(i);
	        	content[i] = getName(row);
	    	}
	    	
	    	return content;
	}
	
	private boolean[][] getRowAbsences(List<Identity> rows, List<LectureBlockRollCall> rollCalls) {
		Map<Identity,LectureBlockRollCall> rollCallMap = new HashMap<>();
		for(LectureBlockRollCall rollCall:rollCalls) {
			rollCallMap.put(rollCall.getIdentity(), rollCall);
		}

		int numOfRows = rows.size();
		
		boolean[][] content = new boolean[numOfRows][];
		for(int i=0; i<numOfRows; i++) {
	        	content[i] = new boolean[numOfLectures];
	        	
	        	LectureBlockRollCall rollCall = rollCallMap.get(rows.get(i));
	        	if(rollCall != null && rollCall.getLecturesAbsentList() != null) {
	        		List<Integer> absences = rollCall.getLecturesAbsentList();
	        		for(int j=0; j<numOfLectures; j++) {
	        			content[i][j] = absences.contains(new Integer(j));
	        		}
	        	} else {
	        		for(int j=0; j<numOfLectures; j++) {
	        			content[i][j] = false;
	        		}
	        	}
	    	}
	    	
	    	return content;
	}
	
	private String[] getRowComment(List<Identity> rows, List<LectureBlockRollCall> rollCalls) {
		Map<Identity,LectureBlockRollCall> rollCallMap = new HashMap<>();
		for(LectureBlockRollCall rollCall:rollCalls) {
			rollCallMap.put(rollCall.getIdentity(), rollCall);
		}

		int numOfRows = rows.size();
		String[] content = new String[numOfRows];
		for(int i=0; i<numOfRows; i++) {
	        	
	        	LectureBlockRollCall rollCall = rollCallMap.get(rows.get(i));
	        	if(rollCall != null && StringHelper.containsNonWhitespace(rollCall.getComment())) {
	        		content[i] = rollCall.getComment();
	        	}
	    	}
	    	return content;
	}
	
	private String getName(Identity identity) {
		StringBuilder sb = new StringBuilder();
		User user = identity.getUser();
		if(StringHelper.containsNonWhitespace(user.getFirstName())) {
			
			sb.append(user.getFirstName());
		}
		if(StringHelper.containsNonWhitespace(user.getLastName())) {
			if(sb.length() > 0) sb.append(" ");
			sb.append(user.getLastName());
		}

		String institutionalIdentifier = user.getProperty(UserConstants.INSTITUTIONALUSERIDENTIFIER, translator.getLocale());
		if(StringHelper.containsNonWhitespace(institutionalIdentifier)) {
			if(sb.length() > 0) sb.append(", ");
			sb.append(institutionalIdentifier);
		}
		return sb.toString();
	}
    
	public int drawTable(String[] content, boolean[][] checked, String[]comments, int offset, float fontSize, float cellMargin)
	throws IOException {
	
		float tableWidth = width;
		float rowHeight = (lineHeightFactory * fontSize) + (2 * cellMargin);
		
		float allColWidth = 25f;
		float lectureColWidth = 15f;
		float lecturesColWidth =  numOfLectures * lectureColWidth + cellMargin;

		float nameMaxSizeWithMargin = (tableWidth - lecturesColWidth - allColWidth) / 2.0f;
		if(nameMaxSizeWithMargin < 140.0f) {
			nameMaxSizeWithMargin = 140.0f;
		}
		float commentColWidth = tableWidth - lecturesColWidth - allColWidth - nameMaxSizeWithMargin;
		float nameMaxSize = nameMaxSizeWithMargin - (2 * cellMargin);
		
		float availableHeight = currentY - marginTopBottom - rowHeight;
		
		float[] rowHeights = new float[content.length];
		float usedHeight = 0.0f;
		int possibleRows = 0;
		for(int i = offset; i < content.length; i++) {
			String name = content[i];
			float nameWidth = getStringWidth(name, fontSize);
			float nameHeight;
			if(nameWidth > nameMaxSize) {
				nameHeight = rowHeight + (lineHeightFactory * fontSize);
			} else {
				nameHeight = rowHeight;
			}

			if((usedHeight + nameHeight) > availableHeight) {
				break;
			}
			usedHeight += nameHeight;
			rowHeights[i] = nameHeight;
			possibleRows++;
		}
		
		int end = Math.min(offset + possibleRows, content.length);
		int rows = end - offset;
		
		float tableHeight = usedHeight + rowHeight;
	
		// draw the horizontal line of the rows
		float y = currentY;
		float nexty = currentY;
		drawLine(marginLeftRight, nexty, marginLeftRight + tableWidth, nexty, 0.5f);
		nexty -= rowHeight;
		for (int i =offset; i < end; i++) {
			drawLine(marginLeftRight, nexty, marginLeftRight + tableWidth, nexty, 0.5f);
			nexty -= rowHeights[i];
		}
		drawLine(marginLeftRight, nexty, marginLeftRight + tableWidth, nexty, 0.5f);

		// draw the vertical line of the columns
		float nextx = marginLeftRight;
		drawLine(nextx, y, nextx, y - tableHeight, 0.5f);
		nextx += nameMaxSizeWithMargin;
		drawLine(nextx, y, nextx, y - tableHeight, 0.5f);
		nextx += lecturesColWidth;
		drawLine(nextx, y, nextx, y - tableHeight, 0.5f);
		nextx += allColWidth;
		drawLine(nextx, y, nextx, y - tableHeight, 0.5f);
		nextx += commentColWidth;
		drawLine(nextx, y, nextx, y - tableHeight, 0.5f);

		// now add the text
		// draw the headers
		final float textx = marginLeftRight + cellMargin;
		float texty = currentY;
		{
			float headerY = texty - rowHeight + (2 * cellMargin);
			float headerX = textx;
			
			currentContentStream.beginText();
			currentContentStream.setFont(fontBold, fontSize);
			currentContentStream.moveTextPositionByAmount(headerX, headerY);
			currentContentStream.drawString(translator.translate("pdf.table.header.participants"));
			currentContentStream.endText();
			
			headerX += nameMaxSizeWithMargin;
			for(int i=0; i<numOfLectures; i++) {
				currentContentStream.beginText();
				currentContentStream.setFont(fontBold, fontSize);
				currentContentStream.moveTextPositionByAmount(headerX, headerY);
				currentContentStream.drawString(Integer.toString(i+1));
				currentContentStream.endText();
				headerX += lectureColWidth;
			}
			
			headerX += cellMargin;
			currentContentStream.beginText();
			currentContentStream.setFont(fontBold, fontSize);
			currentContentStream.moveTextPositionByAmount(headerX, headerY);
			currentContentStream.drawString(translator.translate("pdf.table.header.all"));
			currentContentStream.endText();

			headerX += allColWidth;
			currentContentStream.beginText();
			currentContentStream.setFont(fontBold, fontSize);
			currentContentStream.moveTextPositionByAmount(headerX, headerY);
			currentContentStream.drawString(translator.translate("pdf.table.header.comment"));
			currentContentStream.endText();
		}

		currentY -= rowHeight;

		//draw the content
		texty = currentY - 15;
		for (int i=offset; i<end; i++) {
			String text = content[i];
			if(text == null) continue;
			
			if(rowHeights[i] > rowHeight + 1) {
				//can do 2 lines
				String[] texts = splitText(text, nameMaxSize, fontSize);
				float lineTexty = texty;
				for(int k=0; k<2 && k<texts.length; k++) {
					String textLine = texts[k];
					currentContentStream.beginText();
					currentContentStream.setFont(font, fontSize);
					currentContentStream.moveTextPositionByAmount(textx, lineTexty);
					currentContentStream.drawString(textLine);
					currentContentStream.endText();
					lineTexty -= (lineHeightFactory * fontSize);
				}
			} else {
				currentContentStream.beginText();
				currentContentStream.setFont(font, fontSize);
				currentContentStream.moveTextPositionByAmount(textx, texty);
				currentContentStream.drawString(text);
				currentContentStream.endText();
			}
			
			float offetSetYTop = 7f;
			float offetSetYBottom = 2f;
			float boxWidth = 9.0f;
			
			//absences check box
			boolean all = true;
			boolean[] absences = checked[i];
			float boxx = textx + nameMaxSizeWithMargin;
			for (int j=0; j<absences.length; j++) {
				drawLine(boxx, texty + offetSetYTop, boxx, texty - offetSetYBottom, 0.5f);
				drawLine(boxx, texty - offetSetYBottom, boxx + boxWidth, texty - offetSetYBottom, 0.5f);
				drawLine(boxx, texty + offetSetYTop, boxx + boxWidth, texty + offetSetYTop, 0.5f);
				drawLine(boxx + boxWidth, texty + offetSetYTop, boxx + boxWidth, texty - offetSetYBottom, 0.5f);
				
				if(absences[j]) {
					currentContentStream.beginText();
					currentContentStream.setFont(font, fontSize);
					currentContentStream.moveTextPositionByAmount(boxx + 2f, texty);
					currentContentStream.drawString("x");
					currentContentStream.endText();
				}
				all &= absences[j];
				boxx += 15f;
			}

			{// all check box
				boxx += cellMargin;
				drawLine(boxx, texty + offetSetYTop, boxx, texty - offetSetYBottom, 0.5f);
				drawLine(boxx, texty - offetSetYBottom, boxx + boxWidth, texty - offetSetYBottom, 0.5f);
				drawLine(boxx, texty + offetSetYTop, boxx + boxWidth, texty + offetSetYTop, 0.5f);
				drawLine(boxx + boxWidth, texty + offetSetYTop, boxx + boxWidth, texty - offetSetYBottom, 0.5f);
			
				if(all) {
					currentContentStream.beginText();
					currentContentStream.setFont(font, fontSize);
					currentContentStream.moveTextPositionByAmount(boxx + 2f, texty);
					currentContentStream.drawString("x");
					currentContentStream.endText();
				}
				boxx += allColWidth;
			}
			
			{//comment
				if(comments[i] != null) {
					String comment = comments[i];
					float nameWidth = getStringWidth(comment, fontSize);
					if(nameWidth > commentColWidth) {
						nameWidth = getStringWidth(comment, fontSize - 2);
						if(nameWidth > commentColWidth) {
							//cut
							float numOfChars = comment.length() * (commentColWidth / nameWidth);
							comment = comment.substring(0, Math.round(numOfChars) - 4) + "...";
						}
						currentContentStream.beginText();
						currentContentStream.setFont(font, fontSize - 2);
						currentContentStream.moveTextPositionByAmount(boxx + 2f, texty);
						currentContentStream.drawString(comment);
						currentContentStream.endText();
					} else {
						currentContentStream.beginText();
						currentContentStream.setFont(font, fontSize);
						currentContentStream.moveTextPositionByAmount(boxx + 2f, texty);
						currentContentStream.drawString(comment);
						currentContentStream.endText();
					}
				}
			}
			
			texty -= rowHeights[i];
		}
		return rows;
	}
}