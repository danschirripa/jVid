package com.javashell.video.test;

import java.io.File;
import java.io.IOException;

import com.javashell.video.converters.QOVFileViewer;

public class QOVToPreview {

	public static void main(String[] args) throws IOException, InterruptedException {
		File inputFile = new File(args[0]);
		QOVFileViewer qov = new QOVFileViewer(inputFile);
		qov.playback();
	}

}
