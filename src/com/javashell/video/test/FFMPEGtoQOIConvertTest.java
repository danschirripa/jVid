package com.javashell.video.test;

import java.io.File;
import java.io.IOException;

import com.javashell.video.converters.FFMPEGIngestToQOVFile;

public class FFMPEGtoQOIConvertTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		File inputFile = new File(args[0]);
		String outputFileName = args[1];
		FFMPEGIngestToQOVFile conv = new FFMPEGIngestToQOVFile(inputFile, outputFileName);
		conv.runConversion();
	}

}
