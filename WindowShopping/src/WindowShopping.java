import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovAllKPredictor;

public class WindowShopping {
	public static Path FOLDER = Paths.get("C:/Users/Alex/Downloads/Sports/E-Sports");
	private final Predictor predictor;
	List<Player> ps = new ArrayList<>();

	class Player {
		final int matchId, slot, hero, account;

		private Player(int matchId, int slot, int account, int hero) {
			this.matchId = matchId;
			this.slot = slot;
			this.hero = hero;
			this.account = account;
		}
	}
	private WindowShopping(){
		lines("players.csv", a -> {
			int matchId = Integer.parseInt(a[0]);
			int accountId = Integer.parseInt(a[1]);
			int heroId = Integer.parseInt(a[2]);
			int playerSlot = Integer.parseInt(a[3]);
			ps.add(new Player(matchId, playerSlot, accountId, heroId));
		});
		SequenceDatabase sd = new SequenceDatabase();
		List<Sequence> sds = sd.getSequences();
		int[] passed = { 0 }, match = { 0 }, player = { 0 };
		Sequence[] seq = { new Sequence(-1) };
		int tm = 35000;
		lines("purchase_log.csv", a -> {
			int itemId = Integer.parseInt(a[0]);
			int time = Integer.parseInt(a[1]);
			int playerSlot = Integer.parseInt(a[2]);
			int matchId = Integer.parseInt(a[3]);
			int sid = matchId * 10 + (playerSlot > 4 ? playerSlot - 123 : playerSlot);
			Player p = ps.get(sid);
			if(matchId != match[0]){
				++match[0];
				player[0] = 0;
				if(passed[0]++ == tm) return true;
				sds.add(seq[0]);
				seq[0] = new Sequence(sds.size());
			}
			if(playerSlot != player[0]){
				player[0] = playerSlot;
				if(passed[0]++ == tm) return true;
				sds.add(seq[0]);
				seq[0] = new Sequence(sds.size());
			}
			if(p.account == 0) return false;
			seq[0].addItem(new Item(itemId));
			return false;
		});
		sds.add(seq[0]);
		MarkovAllKPredictor predictionModel = new MarkovAllKPredictor("AKOM", "order:5");
		predictionModel.Train(sds);
		predictor = predictionModel;
	}
	public Sequence predict(Sequence input){
		return predictor.Predict(input);
	}
	public void run(Reader in, PrintWriter out){
		Sequence sequence = new Sequence(-1);
		out.println((int)predict(sequence).get(0).val);
		try(Scanner s = new Scanner(in)){
			for(String l; ; ){
				out.flush();
				l = s.nextLine();
				if(l.isEmpty()){
					out.println((int)predict(sequence).get(0).val);
					continue;
				} else if(l.charAt(0) == '+'){
					try {
						sequence.addItem(new Item(Integer.parseInt(l.substring(1))));
					} catch(NumberFormatException ex){
						out.println("invalid input");
						continue;
					}
					Sequence pred = predict(sequence);
					if(pred.getItems().isEmpty()){
						sequence.getItems().remove(sequence.getItems().size() - 1);
						pred = predict(sequence);
					}
					out.println((int)pred.get(0).val);
					continue;
				} else if(l.charAt(0) == '-'){
					if("-exit".equals(l)) return;
					if("-reset".equals(l)){
						sequence = new Sequence(-1);
						out.println((int)predict(sequence).get(0).val);
						continue;
					}
				}
				out.println("invalid input");
			}
		}
	}
	private static void lines(String file, Consumer<String[]> c) {
		lines(file, a -> {
			c.accept(a);
			return false;
		});
	}
	private static void lines(String file, Predicate<String[]> c) {
		try(BufferedReader r = new BufferedReader(new FileReader(FOLDER.resolve(file).toFile()))){
			String[] a = r.readLine().split(",");
			for(String s; (s = r.readLine()) != null; ){
				int p = -1;
				for(int i = 0; i < a.length - 1; i++){
					a[i] = s.substring(++p, p = s.indexOf(',', p));
				}
				a[a.length - 1] = s.substring(++p, s.length());
				if(c.test(a)) break;
			}
		} catch(IOException e){
			throw new AssertionError(e);
		}
	}
	public static Path getJar(){
		try {
			return Paths.get(WindowShopping.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}
	
	public static void main(String[] args){
		try {
			FOLDER = getJar().getParent();
		} catch(Throwable t){
			
		}
		if(args.length > 1){
			FOLDER = Paths.get(args[1]);
		} else {
		}
		new WindowShopping().run(new InputStreamReader(System.in), new PrintWriter(System.out));
		
		/*lines("patch_dates.csv", a -> {
			patchDates = Arrays.copyOf(patchDates, patchDates.length + 1);
			patchDates[patchDates.length - 1] = DateTimeFormatter.ISO_DATE_TIME.parse(a[0], Instant::from)
					.getEpochSecond();
		});
		patchDates = Arrays.copyOfRange(patchDates, 15, patchDates.length);
		System.out.println(Arrays.toString(patchDates));*/
		//purchaseLogs();
		//playerRatings();
		//abilityUpgrades();
		//System.out.println(player + " " + match);
		//System.out.println(lvl);
		//System.out.println(Arrays.toString(dts));
		//Arrays.sort(dts);
		//HashMap<Integer, Integer> ma = new HashMap<>();
		/*IntStream.range(0, dts.length).forEach(i -> {
			int v = dts[i];
			if(v > 0) ma.put(v, i);
		});*/
		//ma.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> System.out.println(e));
		//System.out.println(ma.size() + " " + lvl);
		//IntStream.range(0, dts.length).forEach(i -> System.out.print(i + ": " + dts[i] + "  "));
	}
	
	
	
	
	//static int[] dts = new int[1030];
	//static long[] patchDates = new long[0];
	static int player, match, ltime, lvl;
	static HashSet<Integer> ms = new HashSet<>();
	private static void match(){
		lines("match.csv", a -> {
			int matchId = Integer.parseInt(a[0]);
			int startTime = Integer.parseInt(a[1]);
			int duration = Integer.parseInt(a[2]);
			int tower_radiant = Integer.parseInt(a[3]);
			int tower_dire = Integer.parseInt(a[4]);
			int barracks_dire = Integer.parseInt(a[5]);
			int barracks_radiant = Integer.parseInt(a[6]);
			int fist_blood_time = Integer.parseInt(a[7]);
			int gameMode = Integer.parseInt(a[8]);
			boolean radiantWin = Boolean.parseBoolean(a[9]);
			int negativeVotes = Integer.parseInt(a[10]);
			int positiveVotes = Integer.parseInt(a[11]);
			int cluster = Integer.parseInt(a[12]);
			if(gameMode != 22) System.err.println(gameMode);
		});
	}
	private static void abilityUpgrades(){
		lines("ability_upgrades.csv", a -> {
			int ability = Integer.parseInt(a[0]);
			int level = Integer.parseInt(a[1]);
			int time = Integer.parseInt(a[2]);
			int playerSlot = Integer.parseInt(a[3]);
			int matchId = Integer.parseInt(a[4]);
			//if(playerSlot < 0) throw new Error("" + playerSlot);
			//if(matchId < 0) throw new Error("" + matchId);
			//if(time <= 0) throw new Error();
			if(matchId != match){
				++match;
				//if(matchId != match) throw new Error("m" + match);
				player = 0;
				ltime = 0;
				lvl = 0;
			}
			if(playerSlot != player){
				//if(playerSlot < player) throw new Error("p " + playerSlot);
				player = playerSlot;
				ltime = 0;
				lvl = 0;
			}
			//if(time < ltime) throw new Error("t " + time);
			if(ltime != 0){
				int dt = time - ltime;
				//if(dt >= dts.length) dts = Arrays.copyOf(dts, dt + 1);
				//dts[dt]++;
			}
			ltime = time;
		});
	}
	private static HashSet<Integer> ids = new HashSet<>(), heroIds = new HashSet<>();
	private static Sequence seq = new Sequence(0);
	private static int[] co = new int[500], wr = new int[500];
	private static void purchaseLogs(){
		/*class Player {
			final int matchId, slot, hero, account;

			private Player(int matchId, int slot, int account, int hero) {
				this.matchId = matchId;
				this.slot = slot;
				this.hero = hero;
				this.account = account;
			}
		}
		List<Player> ps = new ArrayList<>();
		lines("players.csv", a -> {
			int matchId = Integer.parseInt(a[0]);
			int accountId = Integer.parseInt(a[1]);
			int heroId = Integer.parseInt(a[2]);
			int playerSlot = Integer.parseInt(a[3]);
			ps.add(new Player(matchId, playerSlot, accountId, heroId));
			heroIds.add(heroId);
		});
		SequenceDatabase sd = new SequenceDatabase();
		List<Sequence> sds = sd.getSequences();
		int[] passed = { 0 };
		int tm = 350000;
		lines("purchase_log.csv", a -> {
			int itemId = Integer.parseInt(a[0]);
			int time = Integer.parseInt(a[1]);
			int playerSlot = Integer.parseInt(a[2]);
			int matchId = Integer.parseInt(a[3]);
			int sid = matchId * 10 + (playerSlot > 4 ? playerSlot - 123 : playerSlot);
			Player p = ps.get(sid);
			ids.add(itemId);
			if(matchId != match){
				++match;
				player = 0;
				if(passed[0]++ == tm) return true;
				sds.add(seq);
				seq = new Sequence(sds.size());
			}
			if(playerSlot != player){
				player = playerSlot;
				if(passed[0]++ == tm) return true;
				sds.add(seq);
				seq = new Sequence(sds.size());
			}
			if(p.account == 0) return false;
			seq.addItem(new Item(itemId));
			return false;
		});
		sds.add(seq);
		/*Evaluator evaluator = new Evaluator(new DatabaseHelper(""){
			{this.database = sd; }
			public void loadDataset(String fileName, int maxCount) {
				
			}
		});
		evaluator.addDataset("BMS", 		5000);
		
		// Loading predictors
		// Here we will compare 7 predictors.
		evaluator.addPredictor(new DGPredictor("DG", "lookahead:4"));
		evaluator.addPredictor(new TDAGPredictor());
		evaluator.addPredictor(new CPTPlusPredictor("CPT+",		"CCF:true CBS:true"));
		evaluator.addPredictor(new CPTPredictor());
		evaluator.addPredictor(new MarkovFirstOrderPredictor());
		evaluator.addPredictor(new MarkovAllKPredictor());
		evaluator.addPredictor(new LZ78Predictor());
		
		// Start the experiment
		// by using 14-fold cross-validation
		try {
			StatsLogger results = evaluator.Start(Evaluator.KFOLD, 14 , true, true, true);
		} catch (IOException e2) {
			throw new Error(e2);
		}
		if(true) return;*/
		///*
		//String optionalParameters = "CCF:false CBS:false CCFmin:1 CCFmax:6 CCFsup:2 splitMethod:0 splitLength:4 minPredictionRatio:1.0 noiseRatio:1.0";
		//CPTPlusPredictor predictionModel = new CPTPlusPredictor("CPT+", optionalParameters);
		System.out.println("training");
		/*MarkovAllKPredictor predictionModel = new MarkovAllKPredictor("AKOM", "order:5");
		predictionModel.Train(sds);
		System.out.println("testing");
		try {
			SequenceStatsGenerator.prinStats(sd, " training sequences ");
		} catch (IOException e1) {
			throw new Error(e1);
		}*/
		WindowShopping dd = new WindowShopping();
		int[] passed = { 0 };
		seq = new Sequence(0);
		player = match = -1;
		int ml = 200;
		ExecutorService ex = Executors.newFixedThreadPool(3);
		lines("purchase_log.csv", a -> {
			int itemId = Integer.parseInt(a[0]);
			int time = Integer.parseInt(a[1]);
			int playerSlot = Integer.parseInt(a[2]);
			int matchId = Integer.parseInt(a[3]);
			int sid = matchId * 10 + (playerSlot > 4 ? playerSlot - 123 : playerSlot);
			Player p = dd.ps.get(sid);
			if(matchId != match){
				++match;
				player = 0;
				if(passed[0]++ == 400000) return true;
				seq = new Sequence(0);
			}
			if(playerSlot != player){
				player = playerSlot;
				if(passed[0]++ == 400000) return true;
				seq = new Sequence(0);
			}
			if(p.account == 0) return false;
			int si = seq.getItems().size();
			int pa = passed[0];
			if(pa > 350000 && si < ml){
				Sequence seq = new Sequence(WindowShopping.seq);
				ex.submit(() -> {
					try {
						int thePrediction = dd.predictor.Predict(seq).get(0).val;
						synchronized(passed){
							(itemId == thePrediction ? co : wr)[si]++;
							if(si == 1) lvl++;
							if(pa % 1000 != 0 || si != 1) return;
							System.out.print(pa + " ");
							for(int i = 0; i < ml; i++){
								System.out.format("%3d/%3d ", (int)(co[i] / (double)(wr[i] + co[i]) * 1000), wr[i] + co[i]);
							}
							System.out.println();
						}
					} catch(Throwable t){
						t.printStackTrace();
					}
				});
			}
			seq.addItem(new Item(itemId));
			return false;
		});
		ex.shutdown();
		System.out.print(lvl + " ");
		int wt = 0, ct = 0;
		for(int i = 0; i < ml; i++){
			System.out.format("%3d/%3d ", (int)(co[i] / (double)(wr[i] + co[i]) * 1000), wr[i] + co[i]);
			wt += wr[i];
			ct += co[i];
		}
		System.out.println((int)(ct / (double)(wt + ct) * 1000));
		if(true) return;//*/
		passed[0] = 0;
		int[] spassed = { 0 };
		player = match = 0;
		String idss = ids.stream().map(i -> i.toString()).collect(Collectors.joining(","));
		String hids = heroIds.stream().map(i -> i.toString()).collect(Collectors.joining(","));
		try(BufferedWriter w = new BufferedWriter(new FileWriter(FOLDER.resolve("purchases.arff").toFile()))){
			w.write("@RELATION purchases\n");
			w.write("@ATTRIBUTE time INTEGER\n");
			w.write("@ATTRIBUTE index INTEGER\n");
			w.write("@ATTRIBUTE sequence INTEGER\n");
			w.write("@ATTRIBUTE account INTEGER\n");
			w.write("@ATTRIBUTE class {" + hids + "}\n");
			w.write("@ATTRIBUTE item_id {" + idss + "}\n");
			w.write("@DATA\n");
			ltime = -1000;
		lines("purchase_log.csv", a -> {
			int itemId = Integer.parseInt(a[0]);
			int time = Integer.parseInt(a[1]);
			int playerSlot = Integer.parseInt(a[2]);
			int matchId = Integer.parseInt(a[3]);
			int sid = matchId * 10 + (playerSlot > 4 ? playerSlot - 123 : playerSlot);
			Player p = dd.ps.get(sid);
			if(p.matchId != matchId || p.slot != playerSlot) throw new Error();
//			dts[itemId]++;
//			lvl++;
			//if(playerSlot < 0) throw new Error("" + playerSlot);
			//if(matchId < 0) throw new Error("" + matchId);
			if(matchId != match){
				++match;
				//if(matchId != match) throw new Error("m" + match);
				player = 0;
				passed[0] = 0;
				if(spassed[0]++ >= 50000) return true;
				ltime = -1000;
			}
			if(playerSlot != player){
				//if(playerSlot < player) throw new Error("p " + playerSlot);
				player = playerSlot;
				passed[0] = 0;
				spassed[0]++;
				ltime = -1000;
			}
			//if(time < ltime) throw new Error("t " + time);
			if(ltime != -1000){
				//int dt = time - ltime;
				//if(dt >= dts.length) dts = Arrays.copyOf(dts, dt + 1);
				//dts[dt]++;
			}
			ltime = time;
			try {
				w.write(a[1]); w.write(',');
				w.write(Integer.toString(passed[0]++)); w.write(',');
				w.write(Integer.toString(sid)); w.write(',');
				w.write(Integer.toString(p.account)); w.write(',');
				w.write(Integer.toString(p.hero)); w.write(',');
				w.write(a[0]); w.write('\n');
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			return false;
		});
		} catch (IOException e) {
			throw new Error(e);
		}
	}
	private static void playerRatings(){
		lines("player_ratings.csv", a -> {
			int accountId = Integer.parseInt(a[0]);
			int totalWins = Integer.parseInt(a[1]);
			int totalMatches = Integer.parseInt(a[2]);
			double trueskillMu = Double.parseDouble(a[3]);
			double trueskillSigma = Double.parseDouble(a[4]);
			if(totalMatches > 1000){
				System.out.println(totalMatches);
				return;
			}
			//if(totalMatches >= matches.length) matches = Arrays.copyOf(matches, totalMatches + 1);
			//matches[totalMatches]++;
		});
	}
}
