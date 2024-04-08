package com.javashell.video.test;

import java.io.File;
import java.io.IOException;

import com.javashell.video.converters.QOYVFileViewer;

public class QOYVToPreview {

	public static void main(String[] args) throws IOException, InterruptedException {
		File inputFile = new File(args[0]);
		QOYVFileViewer qov = new QOYVFileViewer(inputFile);
		qov.playback();
	}

}
