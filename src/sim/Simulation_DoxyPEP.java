package sim;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

public class Simulation_DoxyPEP extends Simulation_ClusterModelTransmission {

	protected File seedMapFile = null;

	public Simulation_DoxyPEP(File seedMapFile) {
		this.seedMapFile = seedMapFile;

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final String USAGE_INFO = String.format(
				"Usage: java %s - PROP_FILE_DIRECTORY " + "<-export_skip_backup> <-printProgress> <%sSEED_MAP>\n",
				Simulation_DoxyPEP.class.getName(), Simulation_ClusterModelTransmission.LAUNCH_ARGS_SEED_MAP);
		if (args.length < 1) {
			System.out.println(USAGE_INFO);
			System.exit(0);
		} else {
			File seedMapFile = null;
			for (int a = 1; a < args.length; a++) {
				if (args[a].startsWith(Simulation_ClusterModelTransmission.LAUNCH_ARGS_SEED_MAP)) {
					String argEnt = args[a].split("=")[1];
					File predefineSeedMap = new File(new File(args[0]), argEnt);
					if (predefineSeedMap.isFile()) {
						seedMapFile = predefineSeedMap;
					}
				}
			}

			Simulation_DoxyPEP sim = new Simulation_DoxyPEP(seedMapFile);
			Simulation_ClusterModelTransmission.launch(args, sim);
			sim.testZip();
		}
	}

	@Override
	public Abstract_Runnable_ClusterModel_Transmission generateDefaultRunnable(long cMap_seed, long sim_seed,
			Properties loadProperties) {

		// Set output path to be same as seed file
		if (seedMapFile != null) {
			loadProperties.put(Runnable_ClusterModel_MultiTransmission.PROP_SEED_FILE_PATH,
					seedMapFile.getAbsolutePath());
		}

		return new Runnable_ClusterModel_Prophylaxis(cMap_seed, sim_seed, baseContactMapMapping.get(cMap_seed),
				loadedProperties);
	}

	@Override
	protected void finalise_simulations() throws IOException, FileNotFoundException {
		// Skip if seedMapFile
		if (seedMapFile == null) {
			super.finalise_simulations();
		} else {
			Pattern pattern_csv = Pattern.compile("(?:\\[.*\\]){0,1}(.*)_(-{0,1}\\d+)_-{0,1}\\d+.csv");

			File[] csv_all = seedMapFile.getParentFile().listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pattern_csv.matcher(pathname.getName()).matches();
				}
			});

			HashMap<String, ArrayList<File>> filemap_by_type = new HashMap<>();

			for (File csv : csv_all) {
				Matcher m = pattern_csv.matcher(csv.getName());
				m.matches();

				String file_type = m.group(1);
				ArrayList<File> file_ent = filemap_by_type.get(file_type);

				if (file_ent == null) {
					file_ent = new ArrayList<>();
					filemap_by_type.put(file_type, file_ent);
				}
				file_ent.add(csv);
			}

			for (Entry<String, ArrayList<File>> ent : filemap_by_type.entrySet()) {
				File zipFile = new File(seedMapFile.getParentFile(), String.format("Results_%s.7z", ent.getKey()));

				SevenZOutputFile outputZip = new SevenZOutputFile(zipFile);
				// Add new entry to zip
				SevenZArchiveEntry entry;
				FileInputStream fIn;

				for (File element : ent.getValue()) {
					entry = outputZip.createArchiveEntry(element, element.getName());
					outputZip.putArchiveEntry(entry);
					fIn = new FileInputStream(element);
					outputZip.write(fIn);
					outputZip.closeArchiveEntry();
					fIn.close();
				}

				outputZip.close();

				// Clean up
				for (File f : ent.getValue()) {
					f.delete();
				}

			}

		}

	}

	public void testZip() {
		try {
			finalise_simulations();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
