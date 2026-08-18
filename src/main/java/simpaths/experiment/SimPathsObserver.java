// define package
package simpaths.experiment;

// import Java packages
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

// import plug-in packages
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.Validator;
import simpaths.model.enums.Country;
import simpaths.model.enums.Education;
import simpaths.model.enums.EducationLevel;
import simpaths.model.enums.Gender;
import simpaths.model.enums.HistogramTypeEnum;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.Region;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import net.miginfocom.swing.MigLayout;
import microsim.FilteredCollection;
// import JAS-mine packages
import microsim.annotation.GUIparameter;
import microsim.caching.OnceUntil;
import microsim.dev.statistics.CrossSection;
import microsim.dev.statistics.Stats;
import microsim.dev.statistics.WeightedCrossSection;
import microsim.dev.statistics.WeightedStats;
import microsim.engine.AbstractSimulationObserverManager;
import microsim.engine.SimulationCollectorManager;
import microsim.engine.SimulationManager;
import microsim.event.CommonEventType;
import microsim.event.EventGroup;
import microsim.event.EventListener;
import microsim.event.SingleTargetEvent;
import microsim.gui.GuiUtils;
import microsim.gui.plot.IndividualBarSimulationPlotter;
import microsim.gui.plot.ScatterplotSimulationPlotterRefreshable;
import microsim.gui.plot.Weighted_PyramidPlotter;
import microsim.gui.plot.TimeSeriesSimulationPlotter;
import microsim.gui.plot.Weighted_HistogramSimulationPlotter;
import microsim.statistics.weighted.Weighted_CrossSection;
import microsim.statistics.weighted.functions.Weighted_MeanArrayFunction;
import microsim.statistics.weighted.functions.Weighted_SumArrayFunction;

// import LABOURsim packages
import simpaths.model.Person;
import simpaths.data.Parameters;
import simpaths.data.filters.ChildValidIncomeCSfilter;
import simpaths.data.filters.ChildValidIncomeRegionalCSfilter;
import simpaths.data.filters.FemaleAgeGroupCSfilter;
import simpaths.data.filters.FemaleRegionAgeCSfilter;
import simpaths.data.filters.Filters;
import simpaths.data.filters.FlexibleInLabourSupplyByAgeAndGenderFilter;
import simpaths.data.filters.FlexibleInLabourSupplyByEducationFilter;
import simpaths.data.filters.GenderCSfilter;
import simpaths.data.filters.GenderEducationCSfilter;
import simpaths.data.filters.GenderEducationWorkingCSfilter;
import simpaths.data.filters.GenderWorkingCSfilter;
import simpaths.data.filters.MaleAgeGroupCSfilter;
import simpaths.data.filters.MaleRegionAgeCSfilter;
import simpaths.data.filters.RegionCSfilter;
import simpaths.data.filters.RegionEducationWorkingCSfilter;
import simpaths.data.filters.ValidEducationAgeGroupCSfilter;
import simpaths.data.filters.ValidEducationRegionCSfilter;
import simpaths.data.filters.ValidHouseholdIncomeCSfilter;
import simpaths.data.filters.ValidHouseholdIncomeRegionalCSfilter;
import simpaths.data.filters.ValidPersonEarningsCSfilter;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

record AgeRange(int from, int to) implements Predicate<Person> {
    public double employmentValidation(int year, Gender gender) {
        return Parameters.validationEmployment(year, gender, this.from, this.to);
    }

    public double mentalHealthValidation(int year, Gender gender) {
        return Parameters.validationMentalHealth(year, gender, this.from, this.to);
    }

    public double psychDistressValidation(int year, Gender gender) {
        return Parameters.validationPsychDistress(year, gender, this.from, this.to);
    }

    public double lifeSatValidation(int year, Gender gender) {
        return Parameters.validationLifeSatisfaction(year, gender, this.from, this.to);
    }

    public double mcsValidation(int year, Gender gender) {
        return Parameters.validationHealthMcs(year, gender, this.from, this.to);
    }

    public double pcsValidation(int year, Gender gender) {
        return Parameters.validationHealthPcs(year, gender, this.from, this.to);
    }

    public double eduValidation(int year, EducationLevel level) {
        return Parameters.validationEduc(year, level, this.from, this.to);
    }

    public double healthValidation(int year, Gender gender) {
        return Parameters.validationHealth(year, gender, this.from, this.to);
    }

    @Override
    public boolean test(Person arg0) {
        return Filters.ageRange(this.from, this.to).test(arg0);
    }
}

@FunctionalInterface
interface AgeGenderValidation {
    Double apply(AgeRange ar, int year, Gender gender);
}


/**
 *
 * CLASS TO MANAGE OBSERVER OF SIMULATED OUTPUT
 *
 */
public class SimPathsObserver extends AbstractSimulationObserverManager implements EventListener {

	@GUIparameter(description="Toggle to turn all charts on/off")
	private Boolean showCharts = true;

	@GUIparameter(description = "Enable additional charts")
	private Boolean showAdditionalCharts = true;

	@GUIparameter(description = "Enable validation statistics")
	private Boolean showValidationStatistics = true;
	
	@GUIparameter(description = "Set the time-period between chart updates")
	private Double displayFrequency = 1.;
	
//	@GUIparameter(description = "Set the type of histogram to display")		//Histogram types other than Frequency do not work properly with weighted histograms / cross sections
	private HistogramTypeEnum histogramType = HistogramTypeEnum.Frequency;

	@GUIparameter(description = "Set the number of bins to use in the Histograms")
    private Integer numberOfHistogramBins = 100;

//	@GUIparameter(description = "Specify the maximum number of most recent data points to show on the scatterplot of the Bowker norm of labour market demand * supply elasticities")
	private Integer convergenceElasticitiesPlotMaxSamples = 50;

//	@GUIparameter(description = "Specify the maximum number of most recent data points to show on the scatterplot of potential earnings during the convergence process")
	private Integer potentialEarningsPlotMaxSamples = 20;
	
//	@GUIparameter(description = "Specify the maximum number of most recent data points to show on the scatterplot of the aggregate labour demand and supply during the convergence process")
	private Integer labourMarketPlotMaxSamples = 20;
	
	//GUI Parameters to toggle specific charts on/off

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean educationByAge = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean educationByRegion = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean educationOfAdults = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean employmentByAge = true;

//	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean employmentByRegion = false;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean employmentOfAdults = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean femaleEmploymentByMaternity = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean grossEarningsByRegionAndEducation = true;

//	@GUIparameter(description="Toggle to turn chart on/off")
//	private boolean health = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean healthByAge = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean householdComposition = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean incomeHistograms = true;

//	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean populationPyramid = true;
	
//	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean workingHoursPyramid = false;

	@GUIparameter(description = "Toggle to turn chart on/off")
	private boolean securityIndex = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean labourSupply = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean population = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean poverty = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean studentsByAge = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean studentsByRegion = true;

	private boolean activityStatus = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean homeownershipStatus = true;


//	@GUIparameter(description = "Allow convergence plots to float freely in GUI, otherwise contain plots in a frame")
	private boolean floatingConvergencePlots = false;		//Allow convergence plots to float freely in GUI, otherwise contain plots in a frame 

    private ArrayList<AgeRange> decades;
    private ArrayList<AgeRange> healthAgeRanges;

	private ScatterplotSimulationPlotterRefreshable convergenceElasticitiesPlotter;

	Set<JInternalFrame> updateChartSet;

	Set<JComponent> tabSet;

	Set<JInternalFrame> convergencePlots = new LinkedHashSet<JInternalFrame>();
	
	Map<Education, ScatterplotSimulationPlotterRefreshable> labourMarketPlots;
	
	Map<Education, ScatterplotSimulationPlotterRefreshable> potentialEarningsPlots;
	
	MultiKeyMap<Object, Supplier<Double>> meanPotentialEarningsMultiMap;
	
	private long countIterations = 0;

	private SimPathsModel model;

	private Validator validator;

	private int ordering = Parameters.OBSERVER_ORDERING;	//Schedule at the same time as the model and collector events, but with a higher order, so will be fired after the model and collector have updated.


	/**
	 *
	 * CONSTRUCTOR FOR SIMULATION OBSERVER
	 *
	 */
	public SimPathsObserver(SimulationManager manager, SimulationCollectorManager simulationCollectionManager) {
		super(manager, simulationCollectionManager);		
	}

    private void ageGenderPlots(String label,
            List<AgeRange> ageRanges,
            Function<? super Person, ? extends Number> getObservable,
            AgeGenderValidation validation) {
        var engine = this.getEngine();
        var plots = new LinkedHashSet<JInternalFrame>();

        // FIXME: reduce duplication with colorArrayList
        var colors = new ArrayList<Color>();
        colors.add(new Color(162, 56, 255));
        colors.add(new Color(254, 131, 0));

        for (var ar : ageRanges) {
            var inRange = new FilteredCollection<>(this.model::getPersons, ar).oncePerSimTime(engine);
            var males = new FilteredCollection<>(inRange, Filters.male());
            var females = new FilteredCollection<>(inRange, Filters.female());

            var maleCs = new WeightedCrossSection<>(males, getObservable, Person::getWeight);
            var femaleCs = new WeightedCrossSection<>(females, getObservable, Person::getWeight);

            // FIXME: should this be cached? What about validation values?
            var meanMale = OnceUntil.timeChanges(() -> new WeightedStats(maleCs.get()).mean(), engine);
            var meanFemale = OnceUntil.timeChanges(() -> new WeightedStats(femaleCs.get()).mean(), engine);

            Supplier<Double> validMale = () -> validation.apply(ar, this.model.getYear(), Gender.Male);
            Supplier<Double> validFemale = () -> validation.apply(ar, this.model.getYear(), Gender.Female);

            var plotter = new TimeSeriesSimulationPlotter(label + " by age: " + ar.from() + " - " + ar.to(), "");
            plotter.addSource("males", meanMale, colors.get(0), false);
            plotter.addSource("females", meanFemale, colors.get(1), false);
            plotter.addSource("Validation males", validMale, colors.get(0), true);
            plotter.addSource("Validation females", validFemale, colors.get(1), true);

            this.updateChartSet.add(plotter); // set to be updated in buildSchedule method
            plots.add(plotter);
        }
        this.tabSet.add(createScrollPaneFromPlots(plots, label + ": age/gender", 2));
    }


	@Override
	public void buildObjects() {
        var engine = this.getEngine();

		if(showCharts) {
			
			model = (SimPathsModel) getManager();
			final SimPathsCollector collector = (SimPathsCollector) getCollectorManager();
			validator = new Validator();

			//Renderers - these allow different graphs to use different look for the series displayed
			XYLineAndShapeRenderer studentAgeRenderer = new XYLineAndShapeRenderer(); //Set up a new renderer to define series colors for this chart

            // FIXME: cache the filtered population?
            this.decades = new ArrayList<>();
            this.decades.add(new AgeRange(20, 29));
            this.decades.add(new AgeRange(30, 39));
            this.decades.add(new AgeRange(40, 49));
            this.decades.add(new AgeRange(50, 59));

            // FIXME: cache the filtered population?
            this.healthAgeRanges = new ArrayList<>();
            this.healthAgeRanges.add(new AgeRange(0, 49));
            this.healthAgeRanges.add(new AgeRange(50, 74));
            this.healthAgeRanges.add(new AgeRange(75, 100));

			updateChartSet = new LinkedHashSet<JInternalFrame>();	//Set of all charts needed to be scheduled for updating (NOT the convergence plot!)
			tabSet = new LinkedHashSet<JComponent>();		//Set of all JInternalFrames each having a tab.  Each tab frame will potentially contain more than one chart each.
			labourMarketPlots = new LinkedHashMap<Education, ScatterplotSimulationPlotterRefreshable>();
			potentialEarningsPlots = new LinkedHashMap<Education, ScatterplotSimulationPlotterRefreshable>();						
			meanPotentialEarningsMultiMap = MultiKeyMap.multiKeyMap(new LinkedMap<>());
			for(Region region: Parameters.getCountryRegions()) {
				for(Education edu: Education.values()) {
                    // do not cache this one to be able to update during convergence.
                    var filtered = new FilteredCollection<>(model::getPersons,
                            Filters.education(edu).and(Filters.region(region)));
                    var wagesCs = new WeightedCrossSection<>(filtered, Person::getHourlyWageRate1, Person::getWeight);
                    var wstats = WeightedStats.supplier(wagesCs);
                    meanPotentialEarningsMultiMap.put(region, edu, () -> wstats.get().mean());
				}
			}
						

			
			//----------------------------------------------------------------------------------------------------------------------------------------
			//
			//	INTER-TIMESTEP CHARTS FOR CONVERGENCE PROCESS - those that update potentially several times in between 'time-steps' (scheduled events)
			//
			//----------------------------------------------------------------------------------------------------------------------------------------
		    
			//POTENTIAL EARNINGS & LABOUR MARKET CONVERGENCE PLOTS
			int width = 400;
			int height = 300;
			Map<Education, Integer> chartXpos = new LinkedHashMap<>();
			Map<Education, Integer> chartYpos = new LinkedHashMap<>();
			for(Education edu: Education.values()) {
				int x = 450, y = 150;
				if(edu.equals(Education.Medium)) {
					x += width;
				}
				else if(edu.equals(Education.High)) {
					x += width*2;
				}
				chartXpos.put(edu, x);
				chartYpos.put(edu, y);
			}
			for(Education edu: Education.values()) {
				ScatterplotSimulationPlotterRefreshable labourPlot = new ScatterplotSimulationPlotterRefreshable(edu + " skill aggregate labour statistics", "iteration", "Hours per Week");
				labourPlot.setMaxSamples(labourMarketPlotMaxSamples);
		    	labourMarketPlots.put(edu, labourPlot);
		    	if(floatingConvergencePlots) {
		    		GuiUtils.addWindow(labourPlot, chartXpos.get(edu), chartYpos.get(edu), width, height);
		    	}
		    	else {
		    		convergencePlots.add(labourPlot);
		    	}
			}		    
			for(Education edu: Education.values()) {
				ScatterplotSimulationPlotterRefreshable potentialEarningsPlot = new ScatterplotSimulationPlotterRefreshable(edu + " skill mean potential earnings", "iteration", "currency (per hour)");
		    	potentialEarningsPlot.setMaxSamples(potentialEarningsPlotMaxSamples);
		    	potentialEarningsPlots.put(edu, potentialEarningsPlot);
		    	if(floatingConvergencePlots) {
		    		GuiUtils.addWindow(potentialEarningsPlot, chartXpos.get(edu), chartYpos.get(edu) + height, width, height);
		    	}
		    	else {
		    		convergencePlots.add(potentialEarningsPlot);
		    	}
			}
			
			
			//This is the color palette used by graphs in the simulation
			ArrayList<Color> colorArrayList = new ArrayList<>();
			colorArrayList.add(new Color(162,56,255));
			colorArrayList.add(new Color(254, 131, 0));
			colorArrayList.add(new Color(151,144,0));
			colorArrayList.add(new Color(0,144,15));
			colorArrayList.add(new Color(0,53,144));
			colorArrayList.add(new Color(254,0,0));
			colorArrayList.add(new Color(198,0,190));
			colorArrayList.add(new Color(175,0,0));
			colorArrayList.add(new Color(0,0,0));
			colorArrayList.add(new Color(255, 172, 172));
			colorArrayList.add(new Color(255, 186, 132));
			colorArrayList.add(new Color(179, 129, 15));
			colorArrayList.add(new Color(175, 255, 148));
			colorArrayList.add(new Color(86, 173, 153));
			colorArrayList.add(new Color(0, 233, 255));
			
			//POPULATION CHART
			if(population) {

				// POPULATION PYRAMID GRAPH
				if (populationPyramid) {
					Set<JInternalFrame> populationPyramidPlots = new LinkedHashSet<JInternalFrame>();
					Weighted_PyramidPlotter populationAgeGenderPlotter = new Weighted_PyramidPlotter();
                    var males = new FilteredCollection<>(model::getPersons, Filters.male());
                    var females = new FilteredCollection<>(model::getPersons, Filters.female());

                    var malesCs = new WeightedCrossSection<>(males, Person::getDemAge, Person::getWeight)
                            .oncePerSimTime(engine);
                    var femalesCs = new WeightedCrossSection<>(females, Person::getDemAge, Person::getWeight)
                            .oncePerSimTime(engine);

					populationAgeGenderPlotter.setScalingFactor(model.getScalingFactor());
                    populationAgeGenderPlotter.setLeft(malesCs);
                    populationAgeGenderPlotter.setRight(femalesCs);

					updateChartSet.add(populationAgeGenderPlotter);			//Add to set to be updated in buildSchedule method
					populationPyramidPlots.add(populationAgeGenderPlotter);

					tabSet.add(createScrollPaneFromPlots(populationPyramidPlots, "Population Pyramid", 1));
				}

				TimeSeriesSimulationPlotter populationPlotter = new TimeSeriesSimulationPlotter("Population Statistics", "");
				if (showAdditionalCharts) {
                    populationPlotter.addSource("(Scaled) Number of Households, occupants below 80 yo",
                            () -> model.getWeightedNumberOfHouseholds80minus());
                }
                populationPlotter.addSource("(Scaled) Population Size",
                        () -> model.getWeightedNumberOfPersons());

				populationPlotter.setName("Population statistics");
			    updateChartSet.add(populationPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(populationPlotter);
			}

            // Population share by age.
            if(showAdditionalCharts) {
                var agesPopShare = new ArrayList<AgeRange>(8);
                agesPopShare.add(new AgeRange(0, 18));
                agesPopShare.add(new AgeRange(0, 0));
                agesPopShare.add(new AgeRange(2, 10));
                agesPopShare.add(new AgeRange(11, 15));
                agesPopShare.add(new AgeRange(19, 25));
                agesPopShare.add(new AgeRange(40, 59));
                agesPopShare.add(new AgeRange(60, 79));
                agesPopShare.add(new AgeRange(80, 100));

                var popShares = agesPopShare.stream()
                        .map(ar -> new FilteredCollection<>(model::getPersons, ar))
                        .map(fc -> new CrossSection<>(fc, Person::getWeight))
                        .map(cs -> Stats.supplier(cs))
                        .map(s -> OnceUntil.timeChanges(() -> s.get().sum(), engine))
                        .toList();

                var plotter = new TimeSeriesSimulationPlotter("Individuals by age", "");
                for (var i = 0; i < popShares.size(); i++) {
                    var ar = agesPopShare.get(i);
                    var ageStr = ar.from() + "-" + ar.to() + " yo";
                    plotter.addSource(ageStr, popShares.get(i), colorArrayList.get(i), false);
                    if (showValidationStatistics) {
                        plotter.addSource(ageStr + " projection",
                                () -> validator.getPopulationProjectionByAge(ar.from(), ar.to()),
                                colorArrayList.get(i), true);
                    }
                }
                plotter.setName("Individuals by age");
                updateChartSet.add(plotter);
                tabSet.add(plotter);
            }

			if (activityStatus) {
				Weighted_CrossSection.Integer employedCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isEmployed); //Get directly from the enum instead of going through a method
				Weighted_CrossSection.Integer notEmployedCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isNotEmployed);
				Weighted_CrossSection.Integer studentCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isStudent);
				Weighted_CrossSection.Integer retiredCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isRetired);
				Weighted_CrossSection.Integer notEmployedRetiredCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isNotEmployedOrRetired);

				TimeSeriesSimulationPlotter activityStatusPlotter = new TimeSeriesSimulationPlotter("Share of individuals by activity status", "");
				activityStatusPlotter.addSeries("Employed", new Weighted_MeanArrayFunction(employedCS), null, colorArrayList.get(0), false);
				activityStatusPlotter.addSeries("Not Employed / Retired", new Weighted_MeanArrayFunction(notEmployedRetiredCS), null, colorArrayList.get(1), false);
				activityStatusPlotter.addSeries("Not Employed", new Weighted_MeanArrayFunction(notEmployedCS), null, colorArrayList.get(4), false);
				activityStatusPlotter.addSeries("Student", new Weighted_MeanArrayFunction(studentCS), null, colorArrayList.get(2), false);
				activityStatusPlotter.addSeries("Retired", new Weighted_MeanArrayFunction(retiredCS), null, colorArrayList.get(3), false);

				activityStatusPlotter.addSeries("Employed validation", validator, Validator.DoublesVariables.activityStatus_Employed, colorArrayList.get(0), true);
				activityStatusPlotter.addSeries("Not Employed / Retired validation", validator, Validator.DoublesVariables.activityStatus_NotEmployedRetired, colorArrayList.get(1), true);
				activityStatusPlotter.addSeries("Student validation", validator, Validator.DoublesVariables.activityStatus_Student, colorArrayList.get(2), true);

				activityStatusPlotter.setName("Activity status");
				updateChartSet.add(activityStatusPlotter);
				tabSet.add(activityStatusPlotter);
			}

			//HOMEOWNERSHIP STATUS
			if (homeownershipStatus) {
				Weighted_CrossSection.Double homeownersBUsCS = new Weighted_CrossSection.Double(model.getBenefitUnits(), BenefitUnit.Regressors.Homeownership_D);
				TimeSeriesSimulationPlotter homeownershipStatusPlotter = new TimeSeriesSimulationPlotter("Share of benefit units owning homes", "");
				homeownershipStatusPlotter.addSeries("Homeowners", new Weighted_MeanArrayFunction(homeownersBUsCS), null, colorArrayList.get(0), false);
				homeownershipStatusPlotter.addSeries("Homeowners validation", validator, Validator.DoublesVariables.homeownership_BenefitUnit, colorArrayList.get(0), true);
				homeownershipStatusPlotter.setName("Homeownership status");
				updateChartSet.add(homeownershipStatusPlotter);
				tabSet.add(homeownershipStatusPlotter);
			}

            // Student enrollment charts
            if (studentsByAge) {
                var ageRanges = new ArrayList<AgeRange>(8);
                ageRanges.add(new AgeRange(15, 19));
                ageRanges.add(new AgeRange(20, 24));
                ageRanges.add(new AgeRange(25, 29));
                if (showAdditionalCharts) {
                    ageRanges.add(new AgeRange(30, 34));
                    ageRanges.add(new AgeRange(35, 39));
                    ageRanges.add(new AgeRange(40, 59));
                    ageRanges.add(new AgeRange(60, 79));
                    ageRanges.add(new AgeRange(80, 100));
                }

                var studentShares = ageRanges.stream()
                        .map(ar -> new FilteredCollection<>(model::getPersons, ar))
                        .map(fc -> new WeightedCrossSection<>(fc, Person::getStudent, Person::getWeight))
                        .map(cs -> WeightedStats.supplier(cs))
                        .map(ws -> OnceUntil.timeChanges(() -> ws.get().mean(), engine))
                        .toList();

                var plotter = new TimeSeriesSimulationPlotter("Proportion of students by age", "");
                plotter.setRenderer(studentAgeRenderer);
                for (var i = 0; i < studentShares.size(); i++) {
                    var ar = ageRanges.get(i);
                    var ageStr = ar.from() + "-" + ar.to() + " yo";
                    plotter.addSource(ageStr, studentShares.get(i), colorArrayList.get(i), false);
                    if (showValidationStatistics) {
                        plotter.addSource("Validation " + ageStr,
                                () -> Parameters.validationStudents(model.getYear(), ar.from(), ar.to()),
                                colorArrayList.get(i), true);
                    }
                }

                if (showAdditionalCharts) {
                    // Unfiltered student cross-section (nationally, for all ages)
                    var studentCs = new WeightedCrossSection<>(model::getPersons, Person::getStudent, Person::getWeight);
                    var studentWs = WeightedStats.supplier(studentCs);
                    var studentShareAll = OnceUntil.timeChanges(() -> studentWs.get().mean(), engine);
                    plotter.addSource("all ages", studentShareAll, new Color(0, 0, 0), false);
                    if (showValidationStatistics) {
                        plotter.addSource("Validation all ages",
                                () -> Parameters.validationStudents(model.getYear()),
                                new Color(0, 0, 0), true);
                    }
                }

                plotter.setName("Students by age");
                updateChartSet.add(plotter);
                tabSet.add(plotter);
            }

			if(studentsByRegion && showAdditionalCharts) {
			    //Student chart by Region 
			    TimeSeriesSimulationPlotter studentRegionPlotter = new TimeSeriesSimulationPlotter("Proportion of students by region", "");
			    int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
					RegionCSfilter regionFilter = new RegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
					regionCS.setFilter(regionFilter);
			    	studentRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					if (showValidationStatistics) {
						studentRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("studentsByRegion_"+region), colorArrayList.get(colorCounter), true);
					}
					colorCounter++;
			    }		    
			    studentRegionPlotter.setName("Students by region");
			    updateChartSet.add(studentRegionPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(studentRegionPlotter);


			}
			
			//EDUCATION LEVEL CHARTS
		    
		    //Education levels for all adults (18 years old and over)
			if(educationOfAdults) {
				ValidEducationAgeGroupCSfilter over17yoFilter = new ValidEducationAgeGroupCSfilter(18,100);		//So we exclude children
				Weighted_CrossSection.Integer lowEducationAdultCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getLowEducation", true);
				lowEducationAdultCS.setFilter(over17yoFilter);
				Weighted_CrossSection.Integer midEducationAdultCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getMidEducation", true);
				midEducationAdultCS.setFilter(over17yoFilter);
				Weighted_CrossSection.Integer highEducationAdultCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getHighEducation", true);
				highEducationAdultCS.setFilter(over17yoFilter);
			    
				TimeSeriesSimulationPlotter eduPlotter = new TimeSeriesSimulationPlotter("Education level of over-17 yo's \n(excluding students)", "");		//'yo' means "years old"
			    eduPlotter.addSeries("Low", new Weighted_MeanArrayFunction(lowEducationAdultCS), null, colorArrayList.get(0), false);
			    eduPlotter.addSeries("Medium", new Weighted_MeanArrayFunction(midEducationAdultCS), null, colorArrayList.get(1), false);
			    eduPlotter.addSeries("High", new Weighted_MeanArrayFunction(highEducationAdultCS), null, colorArrayList.get(2), false);
			    eduPlotter.setName("Education");
			    updateChartSet.add(eduPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(eduPlotter);

			    if (showValidationStatistics) {
			    	eduPlotter.addSeries("Validation Low", validator, Validator.DoublesVariables.educationLevelLow, colorArrayList.get(0), true);
					eduPlotter.addSeries("Validation Medium", validator, Validator.DoublesVariables.educationLevelMedium, colorArrayList.get(1), true);
					eduPlotter.addSeries("Validation High", validator, Validator.DoublesVariables.educationLevelHigh, colorArrayList.get(2), true);
				}
			}
			
            // Education levels by age groups
            if (educationByAge && showAdditionalCharts) {
                var plots = new LinkedHashSet<JInternalFrame>();
                for (var ar : this.decades) {
                    var filter = ar.and(Filters.employment(Les_c4.Student).negate());
                    var filtered = new FilteredCollection<>(model::getPersons, filter).oncePerSimTime(engine);

                    var lowEduCs = new WeightedCrossSection<>(filtered, Person::getLowEducation, Person::getWeight);
                    var midEduCs = new WeightedCrossSection<>(filtered, Person::getMidEducation, Person::getWeight);
                    var highEduCs = new WeightedCrossSection<>(filtered, Person::getHighEducation, Person::getWeight);

                    var meanLow = OnceUntil.timeChanges(() -> new WeightedStats(lowEduCs.get()).mean(), engine);
                    var meanMid = OnceUntil.timeChanges(() -> new WeightedStats(midEduCs.get()).mean(), engine);
                    var meanHigh = OnceUntil.timeChanges(() -> new WeightedStats(highEduCs.get()).mean(), engine);

                    var plotter = new TimeSeriesSimulationPlotter(
                            "Education level by age: " + ar.from() + " - " + ar.to() + "\n(excluding students)", "");
                    plotter.addSource("low", meanLow, colorArrayList.get(0), false);
                    plotter.addSource("mid", meanMid, colorArrayList.get(1), false);
                    plotter.addSource("high", meanHigh, colorArrayList.get(2), false);

                    if (showValidationStatistics) {
                        Supplier<Double> validLow = () -> ar.eduValidation(this.model.getYear(), EducationLevel.Low);
                        Supplier<Double> validMid = () -> ar.eduValidation(this.model.getYear(), EducationLevel.Medium);
                        Supplier<Double> validHigh = () -> ar.eduValidation(this.model.getYear(), EducationLevel.High);
                        plotter.addSource("Validation Low", validLow, colorArrayList.get(0), true);
                        plotter.addSource("Validation Medium", validMid, colorArrayList.get(1), true);
                        plotter.addSource("Validation High", validMid, colorArrayList.get(2), true);
                    }

                    updateChartSet.add(plotter);
                    plots.add(plotter);
                }
                tabSet.add(createScrollPaneFromPlots(plots, "Education by age", 2));
            }

		    //Low & High Education By Region
			if(educationByRegion && showAdditionalCharts) {
			    Set<JInternalFrame> eduLowHighRegionalPlots = new LinkedHashSet<JInternalFrame>();
	
			    //Low Education by region
			    TimeSeriesSimulationPlotter eduLowRegionPlotter = new TimeSeriesSimulationPlotter("Low education level by region", "");
				int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
					ValidEducationRegionCSfilter regionFilter = new ValidEducationRegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getLowEducation", true);
					regionCS.setFilter(regionFilter);
					eduLowRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					eduLowRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("educationLevelLowByRegion_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    			    
			    updateChartSet.add(eduLowRegionPlotter);			//Add to set to be updated in buildSchedule method
			    eduLowHighRegionalPlots.add(eduLowRegionPlotter);
			    
			    //High Education by region
			    TimeSeriesSimulationPlotter eduHighRegionPlotter = new TimeSeriesSimulationPlotter("High education level by region", "");
			    colorCounter = 0; //Reset the color counter
			    for(Region region: Parameters.getCountryRegions()) {
					ValidEducationRegionCSfilter regionFilter = new ValidEducationRegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getHighEducation", true);
					regionCS.setFilter(regionFilter);
					eduHighRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					eduHighRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("educationLevelHighByRegion_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    			    
			    updateChartSet.add(eduHighRegionPlotter);			//Add to set to be updated in buildSchedule method
			    eduLowHighRegionalPlots.add(eduHighRegionPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(eduLowHighRegionalPlots, "Education by region (excluding students)", 2));
			}		    
		    
		    
		    //HOUSEHOLD COMPOSITION CHART
		    if(householdComposition) {
			    //Proportion of households with couple occupancy (i.e. there is both a responsible male and female in the household) by region
			    TimeSeriesSimulationPlotter houseCompositionRegionPlotter = new TimeSeriesSimulationPlotter("Share of couples", "");
				int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
					RegionCSfilter regionFilter = new RegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getCoupleDummy", true);
					regionCS.setFilter(regionFilter);
					houseCompositionRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					houseCompositionRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("partneredShare_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    
			    Weighted_CrossSection.Integer coupleCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getCoupleDummy", true);
			    houseCompositionRegionPlotter.addSeries("national", new Weighted_MeanArrayFunction(coupleCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
				houseCompositionRegionPlotter.addSeries("Validation national", validator, Validator.DoublesVariables.valueOf("partneredShare_All"), colorArrayList.get(colorCounter), true);
			    houseCompositionRegionPlotter.setName("Cohabitation status");
			    updateChartSet.add(houseCompositionRegionPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(houseCompositionRegionPlotter);
		    }

			//Number of males and females who want to cohabit
			if (householdComposition) {
				TimeSeriesSimulationPlotter cohabitationDesireByGender = new TimeSeriesSimulationPlotter("Individuals looking for partner, by gender", "");
				Weighted_CrossSection.Integer toBePartneredMales = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isToBePartnered);
				Weighted_CrossSection.Integer toBePartneredFemales = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isToBePartnered);
				toBePartneredMales.setFilter(new GenderCSfilter(Gender.Male));
				toBePartneredFemales.setFilter(new GenderCSfilter(Gender.Female));
				cohabitationDesireByGender.addSeries("Males", new Weighted_SumArrayFunction.Integer(toBePartneredMales), null, colorArrayList.get(0), false);
				cohabitationDesireByGender.addSeries("Females", new Weighted_SumArrayFunction.Integer(toBePartneredFemales), null, colorArrayList.get(1), false);
				cohabitationDesireByGender.setName("Individuals looking for partner");
				updateChartSet.add(cohabitationDesireByGender);
				tabSet.add(cohabitationDesireByGender);
			}
		    
			MaleAgeGroupCSfilter males18_64Filter = new MaleAgeGroupCSfilter(18, 64);
			FemaleAgeGroupCSfilter females18_64Filter = new FemaleAgeGroupCSfilter(18, 64);

		    //HEALTH CHARTS
			
		    //Male/Female health by age groups
		    if(healthByAge) {
				Set<JInternalFrame> disabledAgePlots = new LinkedHashSet<>();

				MaleAgeGroupCSfilter maleAgeFilterDisabled = new MaleAgeGroupCSfilter(16, 100);
				FemaleAgeGroupCSfilter femaleAgeFilterDisabled = new FemaleAgeGroupCSfilter(16, 100);
				Weighted_CrossSection.Integer maleCSDisabled = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getBadHealth", true);
				maleCSDisabled.setFilter(maleAgeFilterDisabled);
				Weighted_CrossSection.Integer femaleCSDisabled = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getBadHealth", true);
				femaleCSDisabled.setFilter(femaleAgeFilterDisabled);

				TimeSeriesSimulationPlotter disabledAgePlotter = new TimeSeriesSimulationPlotter("Disability rate", "");
				disabledAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCSDisabled), null, colorArrayList.get(0), false);
				disabledAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCSDisabled), null, colorArrayList.get(1), false);
				disabledAgePlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("disabledMale"), colorArrayList.get(0), true);
				disabledAgePlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("disabledFemale"), colorArrayList.get(1), true);

				updateChartSet.add(disabledAgePlotter);
				disabledAgePlots.add(disabledAgePlotter);
				//		}


				tabSet.add(createScrollPaneFromPlots(disabledAgePlots, "Disability: gender", 2));

                ageGenderPlots("Health score", this.healthAgeRanges, Person::getHealthSelfRatedValue, AgeRange::healthValidation);

                // mental health plots
                ageGenderPlots("Psychological distress score", this.decades, Person::getHealthWbScore0to36, AgeRange::mentalHealthValidation);
                ageGenderPlots("Share in psychological distress (case-based)", this.decades, Person::isPsychologicallyDistressed, AgeRange::psychDistressValidation);

                // Psychological distress (case-based) by education
                var psychologicalDistressCasesAgeEducationPlots = new LinkedHashSet<JInternalFrame>();
                for (Education education : Education.values()) {
                    for (var ar : this.decades) {
                        var withEduInAgeRange = new FilteredCollection<>(model::getPersons, ar.and(Filters.education(education))).oncePerSimTime(engine);
                        var males = new FilteredCollection<>(withEduInAgeRange, Filters.male());
                        var females = new FilteredCollection<>(withEduInAgeRange, Filters.female());

                        var maleCs = new WeightedCrossSection<>(males, Person::isPsychologicallyDistressed, Person::getWeight);
                        var femaleCs = new WeightedCrossSection<>(females, Person::isPsychologicallyDistressed, Person::getWeight);

                        // FIXME: should this be cached? What about validation values?
                        var meanMale = OnceUntil.timeChanges(() -> new WeightedStats(maleCs.get()).mean(), engine);
                        var meanFemale = OnceUntil.timeChanges(() -> new WeightedStats(femaleCs.get()).mean(), engine);

                        Supplier<Double> validMale = () -> ar.psychDistressValidation(model.getYear(), Gender.Male);
                        Supplier<Double> validFemale = () -> ar.psychDistressValidation(model.getYear(), Gender.Female);

                        var plotter = new TimeSeriesSimulationPlotter("Share in psychological distress by age: " + ar.from() + " - " + ar.to(), "");
                        plotter.addSource("males " + education + " educ", meanMale, colorArrayList.get(0), false);
                        plotter.addSource("females " + education + " educ", meanFemale, colorArrayList.get(1), false);
                        plotter.addSource("Validation males", validMale, colorArrayList.get(0), true);
                        plotter.addSource("Validation females", validFemale, colorArrayList.get(1), true);
                        updateChartSet.add(plotter);
                        psychologicalDistressCasesAgeEducationPlots.add(plotter);
                    }
                }
                tabSet.add(createScrollPaneFromPlots(psychologicalDistressCasesAgeEducationPlots, "Share in psychological distress (case-based): age/gender/education", 2));

                // Psychological distress (case-based) by education
                var psychologicalDistressCasesEducationPlots = new LinkedHashSet<JInternalFrame>();
                for (Education education : Education.values()) {
                    var filter = Filters.ageRange(25, 64).and(Filters.education(education));
                    var withEduInAgeRange = new FilteredCollection<>(model::getPersons, filter).oncePerSimTime(engine);

                    var males = new FilteredCollection<>(withEduInAgeRange, Filters.male());
                    var females = new FilteredCollection<>(withEduInAgeRange, Filters.female());

                    var maleCs = new WeightedCrossSection<>(males, Person::isPsychologicallyDistressed, Person::getWeight);
                    var femaleCs = new WeightedCrossSection<>(females, Person::isPsychologicallyDistressed, Person::getWeight);

                    // FIXME: should this be cached? What about validation values?
                    var meanMale = OnceUntil.timeChanges(() -> new WeightedStats(maleCs.get()).mean(), engine);
                    var meanFemale = OnceUntil.timeChanges(() -> new WeightedStats(femaleCs.get()).mean(), engine);

                    var plotter = new TimeSeriesSimulationPlotter("Share in psychological distress by education:", "");
                    plotter.addSource("males " + education + " educ", meanMale, colorArrayList.get(0), false);
                    plotter.addSource("females " + education + " educ", meanFemale, colorArrayList.get(1), false);

                    updateChartSet.add(plotter);
                    psychologicalDistressCasesEducationPlots.add(plotter);
                }
                tabSet.add(createScrollPaneFromPlots(psychologicalDistressCasesEducationPlots, "Share in psychological distress (case-based): gender/education", 2));

                ageGenderPlots("Life satisfaction score", this.decades, Person::getDemLifeSatScore0to10, AgeRange::lifeSatValidation);
                ageGenderPlots("Mental health MCS score", this.decades, Person::getHealthMentalMcs, AgeRange::mcsValidation);
                ageGenderPlots("Physical health PCS score", this.decades, Person::getHealthPhysicalPcs, AgeRange::pcsValidation);
		    }
		    
		    
		    //EMPLOYMENT CHARTS
		    if(employmentOfAdults) {
//				MaleAgeGroupCSfilter males18_64Filter = new MaleAgeGroupCSfilter(18, 64);
				FlexibleInLabourSupplyByAgeAndGenderFilter maleAgeFilter = new FlexibleInLabourSupplyByAgeAndGenderFilter(18, 64, Gender.Male);
				Weighted_CrossSection.Integer males18_64CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
				males18_64CS.setFilter(males18_64Filter);
				
				FlexibleInLabourSupplyByAgeAndGenderFilter femaleAgeFilter = new FlexibleInLabourSupplyByAgeAndGenderFilter(18, 64, Gender.Female);
				Weighted_CrossSection.Integer females18_64CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
				females18_64CS.setFilter(females18_64Filter);

				TimeSeriesSimulationPlotter emplPlotter = new TimeSeriesSimulationPlotter("Employment rate (18 - 64)", "");
			    emplPlotter.addSeries("males", new Weighted_MeanArrayFunction(males18_64CS), null, colorArrayList.get(0), false);
			    emplPlotter.addSeries("females", new Weighted_MeanArrayFunction(females18_64CS), null, colorArrayList.get(1), false);
			    emplPlotter.addSeries("Validation males", validator, Validator.DoublesVariables.employmentMale, colorArrayList.get(0), true);
				emplPlotter.addSeries("Validation females", validator, Validator.DoublesVariables.employmentFemale, colorArrayList.get(1), true);

			    emplPlotter.setName("Employment");
				updateChartSet.add(emplPlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(emplPlotter);
		    }

            // Male/Female employment rates by age groups
            if(employmentByAge) {
                ageGenderPlots("Employment rate", this.decades, Person::getEmployed, AgeRange::employmentValidation);
            }

            // One graph for employment age by maternity status, conditional on age of children
            if (femaleEmploymentByMaternity) {
                var emplAgeMaternityPlots = new LinkedHashSet<JInternalFrame>();

                var femalesInAgeRange = new FilteredCollection<>(model::getPersons,
                        Filters.female().and(Filters.ageRange(20, 65))).oncePerSimTime(engine);
                // FIXME: include 18 or not?
                var with0_5 = new FilteredCollection<>(femalesInAgeRange, Filters.hasChildInAgeRange(0, 5));
                var with6_18 = new FilteredCollection<>(femalesInAgeRange, Filters.hasChildInAgeRange(6, 18));
                var without = new FilteredCollection<>(femalesInAgeRange, Filters.hasChildInAgeRange(0, 17).negate());
                var empRates = List.of(with0_5, with6_18, without).stream()
                        .map(fc -> new WeightedCrossSection<>(fc, Person::getEmployed, Person::getWeight))
                        .map(wcs -> WeightedStats.supplier(wcs))
                        .map(ws -> OnceUntil.timeChanges(() -> ws.get().mean(), engine))
                        .toList();

                var plotter = new TimeSeriesSimulationPlotter("Female employment rate, by age of children \n Women aged 20 - 65", "");
                plotter.addSource("with children aged 0 - 5 yo", empRates.get(0), colorArrayList.get(0), false);
                plotter.addSource("with children aged 6 - 18 yo", empRates.get(1), colorArrayList.get(1), false);
                plotter.addSource("without children under 18 yo", empRates.get(2), colorArrayList.get(2), false);
                plotter.addSource("Validation with children aged 0 - 5 yo",
                        () -> Parameters.validationEmployment(model.getYear(), true, true),
                        colorArrayList.get(0), true);
                plotter.addSource("Validation with children aged 6 - 18 yo",
                        () -> Parameters.validationEmployment(model.getYear(), true, false),
                        colorArrayList.get(1), true);
                plotter.addSource("Validation without children under 18 yo",
                        () -> Parameters.validationEmployment(model.getYear(), false, false),
                        colorArrayList.get(2), true);

                updateChartSet.add(plotter);
                emplAgeMaternityPlots.add(plotter);
                tabSet.add(createScrollPaneFromPlots(emplAgeMaternityPlots, "Employment (female): age/maternity", 2));
            }

		    //Employment by region
		    if(employmentByRegion) {
			    Set<JInternalFrame> emplGenderRegionPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter emplMaleRegionPlotter = new TimeSeriesSimulationPlotter("Male employment rate by region\n Age 18 - 64", "");
			    TimeSeriesSimulationPlotter emplFemaleRegionPlotter = new TimeSeriesSimulationPlotter("Female employment rate by region\n Age 18 - 64", "");
			    int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
//					MaleRegionCSfilter maleRegionFilter = new MaleRegionCSfilter(region);
					MaleRegionAgeCSfilter maleRegionFilter = new MaleRegionAgeCSfilter(region, 18, 64);
					Weighted_CrossSection.Integer maleRegionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
					maleRegionCS.setFilter(maleRegionFilter);
					emplMaleRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(maleRegionCS), null, colorArrayList.get(colorCounter), false);
					emplMaleRegionPlotter.addSeries("Validation " + region.getName(), validator, Validator.DoublesVariables.valueOf("employed_male_"+region), colorArrayList.get(colorCounter), true);
					
//					FemaleRegionCSfilter femaleRegionFilter = new FemaleRegionCSfilter(region);
					FemaleRegionAgeCSfilter femaleRegionFilter = new FemaleRegionAgeCSfilter(region, 18, 64);
					Weighted_CrossSection.Integer femaleRegionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
					femaleRegionCS.setFilter(femaleRegionFilter);
					emplFemaleRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(femaleRegionCS), null, colorArrayList.get(colorCounter), false);
					emplFemaleRegionPlotter.addSeries("Validation " + region.getName(), validator, Validator.DoublesVariables.valueOf("employed_female_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    		    
				updateChartSet.add(emplMaleRegionPlotter);			//Add to set to be updated in buildSchedule method		    
				updateChartSet.add(emplFemaleRegionPlotter);			//Add to set to be updated in buildSchedule method
				emplGenderRegionPlots.add(emplFemaleRegionPlotter);
				emplGenderRegionPlots.add(emplMaleRegionPlotter);
				tabSet.add(createScrollPaneFromPlots(emplGenderRegionPlots, "Employment: gender/region", 2));
		    }
		    
		    //LABOUR SUPPLY CHART
		    if(labourSupply) {
				TimeSeriesSimulationPlotter supplyPlotter = new TimeSeriesSimulationPlotter("Labour supply by education", "Yearly hours worked");		//'yo' means "years old"
				int colorCounter = 0;
				for(Education edu: Education.values()) {
					if (Education.InEducation.equals(edu)) {
						continue;
					}
					FlexibleInLabourSupplyByEducationFilter eduFilter = new FlexibleInLabourSupplyByEducationFilter(edu);
					Weighted_CrossSection.Double supplyCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getLabourSupplyHoursYearly", true);
					supplyCS.setFilter(eduFilter);
					supplyPlotter.addSeries(edu.toString(), new Weighted_MeanArrayFunction(supplyCS), null, colorArrayList.get(colorCounter), false);
					supplyPlotter.addSeries("Validation " + edu.toString(), validator, Validator.DoublesVariables.valueOf("labour_supply_"+edu), colorArrayList.get(colorCounter), true);
					colorCounter++;
				}
				supplyPlotter.setName("Labour supply");
			    updateChartSet.add(supplyPlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(supplyPlotter);
		    }

			//INCOME CHARTS - GROSS WAGES BY REGION AND EDUCATION LEVEL
		    if(grossEarningsByRegionAndEducation) {
		    	IndividualBarSimulationPlotter earningsPlotter;
		    	if (model.getCountry().equals(Country.UK)) {
					earningsPlotter = new IndividualBarSimulationPlotter("Yearly Gross Earnings by Education and Region (excludes non-workers)", "£");
				}
		    	else {
					earningsPlotter = new IndividualBarSimulationPlotter("Yearly Gross Earnings by Education and Region (excludes non-workers)", "Euro");
				}

					for(Region region: Parameters.getCountryRegions()) {
			    		for(Education edu: Education.values()) {
							if (Education.InEducation.equals(edu)) {
								continue;
							}
							RegionEducationWorkingCSfilter regionEduWorkingFilter = new RegionEducationWorkingCSfilter(region, edu);
							Weighted_CrossSection.Double wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getGrossEarningsYearly", true);
							wagesCS.setFilter(regionEduWorkingFilter);
							earningsPlotter.addSources("(" + region.getName() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(wagesCS), colorOfEducation(edu));
						}
					}
				earningsPlotter.setName("Gross Earnings");
			    updateChartSet.add(earningsPlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(earningsPlotter);
		    }

			//INCOME CHARTS B: GROSS EARNINGS BY EDUCATION
			if (grossEarningsByRegionAndEducation) {
				TimeSeriesSimulationPlotter grossEarningsByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					grossEarningsByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Yearly Gross Earnings by Gender And Education", "£");
				}
				else {
					grossEarningsByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Yearly Gross Earnings by Gender And Education", "Euro");
				}
					for(Education edu: Education.values()) {
						if (Education.InEducation.equals(edu)) {
							continue;
						}
						for (Gender gender : Gender.values()) {
							GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
							Weighted_CrossSection.Double wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getGrossEarningsYearly", true); // Note: these are nominal values for each simulated year
							wagesCS.setFilter(genderEducationWorkingFilter);
							grossEarningsByGenderAndEducationPlotter.addSeries("(" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(wagesCS), null, colorArrayList.get(colorCounter), false);
							grossEarningsByGenderAndEducationPlotter.addSeries("Validation (" + gender + ", " + edu + ")", validator, Validator.DoublesVariables.valueOf("grossEarnings_"+ gender +"_"+ edu), colorArrayList.get(colorCounter), true);
							colorCounter++;
						}
					}
				grossEarningsByGenderAndEducationPlotter.setName("Gross Earnings by Gender / Education");
				updateChartSet.add(grossEarningsByGenderAndEducationPlotter);
				tabSet.add(grossEarningsByGenderAndEducationPlotter);
			}

			if (grossEarningsByRegionAndEducation) {
				TimeSeriesSimulationPlotter hourlyWagesByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					hourlyWagesByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Hourly Wages by Gender And Education", "£");
				}
				else {
					hourlyWagesByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Hourly Wages by Gender And Education", "Euro");
				}
					for(Education edu: Education.values()) {
						if (Education.InEducation.equals(edu)) {
							continue;
						}
						for (Gender gender : Gender.values()) {
							GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
							Weighted_CrossSection.Double wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getHourlyWageRate1", true); // Note: these are nominal values for each simulated year
							wagesCS.setFilter(genderEducationWorkingFilter);
							hourlyWagesByGenderAndEducationPlotter.addSeries("(" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(wagesCS), null, colorArrayList.get(colorCounter), false);
							hourlyWagesByGenderAndEducationPlotter.addSeries("Validation (" + gender + ", " + edu + ")", validator, Validator.DoublesVariables.valueOf("hourlyWage_"+ gender +"_"+ edu), colorArrayList.get(colorCounter), true);
							colorCounter++;
						}
					}
				hourlyWagesByGenderAndEducationPlotter.setName("Hourly Wages by Gender / Education");
				updateChartSet.add(hourlyWagesByGenderAndEducationPlotter);
				tabSet.add(hourlyWagesByGenderAndEducationPlotter);
			}

			if (grossEarningsByRegionAndEducation) {
				TimeSeriesSimulationPlotter hoursOfWorkByGenderPlotter;
				int colorCounter = 0;
				hoursOfWorkByGenderPlotter = new TimeSeriesSimulationPlotter("Hours of Work Weekly by Gender", "Hours");
				for (Gender gender : Gender.values()) {
					GenderWorkingCSfilter genderWorkingFilter = new GenderWorkingCSfilter(gender);
					Weighted_CrossSection.Double hoursCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDoubleLabourSupplyHoursWeekly", true); // Note: these are nominal values for each simulated year
					hoursCS.setFilter(genderWorkingFilter);
					hoursOfWorkByGenderPlotter.addSeries(gender.toString(), new Weighted_MeanArrayFunction(hoursCS), null, colorArrayList.get(colorCounter), false);
					hoursOfWorkByGenderPlotter.addSeries("Validation " + gender, validator, Validator.DoublesVariables.valueOf("lhw_"+ gender), colorArrayList.get(colorCounter), true);
					colorCounter++;
					}
				hoursOfWorkByGenderPlotter.setName("Hours of Work by Gender");
				updateChartSet.add(hoursOfWorkByGenderPlotter);
				tabSet.add(hoursOfWorkByGenderPlotter);
			}
		    
			//Statistics dependent charts
		    if(collector.isCalculateGiniCoefficients()) {	//As these charts need statistics to be calculated within the simulation, turn off these charts if the statistics are not calculated
		    	
                // FIXME: check what should be plotted here out of the AccumulatorStats
                // and if `lastValue` there is no need to use AccumulatorStats...

				//INCOME CHARTS - GINI
			    Set<JInternalFrame> giniIncomeRegionPlots = new LinkedHashSet<JInternalFrame>();			    
			    //Gini coefficient of market (gross) individual income
			    TimeSeriesSimulationPlotter personalGrossEarningsGiniPlotter = new TimeSeriesSimulationPlotter("Gini: Gross individual earnings", "Gini coefficient");
			    //Add Series at national and regional level
			    for(Region region: Parameters.getCountryRegions()) {
			    	personalGrossEarningsGiniPlotter.addSource(region.getName(), () -> collector.fGiniPersonalGrossEarningsRegionalMap.get(region).lastValue());
			    }
			    personalGrossEarningsGiniPlotter.addSource("national", collector.fGiniPersonalGrossEarningsNational::lastValue);
			    updateChartSet.add(personalGrossEarningsGiniPlotter);			//Add to set to be updated in buildSchedule method
			    giniIncomeRegionPlots.add(personalGrossEarningsGiniPlotter);
			    
			    //Gini coefficient of equivalised household disposable income
			    TimeSeriesSimulationPlotter equivalisedHouseholdDisposableIncomeGiniPlotter = new TimeSeriesSimulationPlotter("Gini: Equivalised household disposable income", "Gini coefficient");
			    //Add Series at national and regional level
			    for(Region region: Parameters.getCountryRegions()) {
			    	equivalisedHouseholdDisposableIncomeGiniPlotter.addSource(region.getName(), () -> collector.fGiniEquivalisedHouseholdDisposableIncomeRegionalMap.get(region).lastValue());
			    }
			    equivalisedHouseholdDisposableIncomeGiniPlotter.addSource("national", collector.fGiniEquivalisedHouseholdDisposableIncomeNational::lastValue);
			    updateChartSet.add(equivalisedHouseholdDisposableIncomeGiniPlotter);			//Add to set to be updated in buildSchedule method		    
			    giniIncomeRegionPlots.add(equivalisedHouseholdDisposableIncomeGiniPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(giniIncomeRegionPlots, "Gini income", 2));
		    }			
			
		    
		    //POVERTY CHARTS
		    if(poverty) { 
			    Set<JInternalFrame> povertyPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter housePovertyPlotter = new TimeSeriesSimulationPlotter("Share of Households at risk of poverty", "");
			    TimeSeriesSimulationPlotter childPovertyPlotter = new TimeSeriesSimulationPlotter("Share of Children at risk of poverty", "");
			    for(Region region: Parameters.getCountryRegions()) {
			    	//Households
					ValidHouseholdIncomeRegionalCSfilter validHouseholdIncomeRegionalFilter = new ValidHouseholdIncomeRegionalCSfilter(region);				
					Weighted_CrossSection.Integer validHousesAtRiskOfPovertyRegionCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getYPvrtyFlag", true);
					validHousesAtRiskOfPovertyRegionCS.setFilter(validHouseholdIncomeRegionalFilter);
					housePovertyPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(validHousesAtRiskOfPovertyRegionCS));
					
					//Children
					ChildValidIncomeRegionalCSfilter childValidIncomeRegionalFilter = new ChildValidIncomeRegionalCSfilter(region);				
					Weighted_CrossSection.Integer childAtRiskOfPovertyRegionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getAtRiskOfPoverty", true);
					childAtRiskOfPovertyRegionCS.setFilter(childValidIncomeRegionalFilter);
					childPovertyPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(childAtRiskOfPovertyRegionCS));		    		
			    }
			    //Households
			    ValidHouseholdIncomeCSfilter validHouseholdIncomeFilter = new ValidHouseholdIncomeCSfilter();
			    Weighted_CrossSection.Integer validHousesAtRiskOfPovertyCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getYPvrtyFlag", true);
			    validHousesAtRiskOfPovertyCS.setFilter(validHouseholdIncomeFilter);
			    housePovertyPlotter.addSeries("national", new Weighted_MeanArrayFunction(validHousesAtRiskOfPovertyCS));		    
				updateChartSet.add(housePovertyPlotter);			//Add to set to be updated in buildSchedule method
				povertyPlots.add(housePovertyPlotter);
				
				//Children
				ChildValidIncomeCSfilter childValidIncomeFilter = new ChildValidIncomeCSfilter();				
				Weighted_CrossSection.Integer childAtRiskOfPovertyCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getAtRiskOfPoverty", true);
				childAtRiskOfPovertyCS.setFilter(childValidIncomeFilter);
				childPovertyPlotter.addSeries("national", new Weighted_MeanArrayFunction(childAtRiskOfPovertyCS));		    				    
				updateChartSet.add(childPovertyPlotter);			//Add to set to be updated in buildSchedule method
			    povertyPlots.add(childPovertyPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(povertyPlots, "Poverty", 2));
		    }
		    
		    // HISTOGRAMS OF INCOME
		    if(incomeHistograms) {
			    Set<JInternalFrame> histogramIncomePlots = new LinkedHashSet<JInternalFrame>();
			    
			    ValidPersonEarningsCSfilter validEarningsFilter = new ValidPersonEarningsCSfilter();
			    Weighted_HistogramSimulationPlotter grossEarningsHistPlotter = new Weighted_HistogramSimulationPlotter("Individual Gross Earnings (yearly)", "Euro", histogramType.getHistogramType(), numberOfHistogramBins);
			    Weighted_CrossSection.Double grossEarningsCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getGrossEarningsYearly", true);
			    grossEarningsCS.setFilter(validEarningsFilter);
			    
			    grossEarningsHistPlotter.addCollectionSource("Gross Earnings", grossEarningsCS);
			    updateChartSet.add(grossEarningsHistPlotter);			//Add to set to be updated in buildSchedule method
			    histogramIncomePlots.add(grossEarningsHistPlotter);
			    
			    ValidHouseholdIncomeCSfilter validHouseholdIncomeFilter = new ValidHouseholdIncomeCSfilter();
			    Weighted_HistogramSimulationPlotter dispIncomeHistPlotter = new Weighted_HistogramSimulationPlotter("Equivalised Disposable Income of Benefit Unit (yearly)", "Euro", histogramType.getHistogramType(), numberOfHistogramBins);
			    Weighted_CrossSection.Double equivalisedDisposableIncomeCS = new Weighted_CrossSection.Double(model.getBenefitUnits(), BenefitUnit.class, "getEquivalisedDisposableIncomeYearly", true);
			    equivalisedDisposableIncomeCS.setFilter(validHouseholdIncomeFilter);
			    dispIncomeHistPlotter.addCollectionSource("Equivalised BenefitUnit Disposable Income", equivalisedDisposableIncomeCS);
			    updateChartSet.add(dispIncomeHistPlotter);			//Add to set to be updated in buildSchedule method
			    histogramIncomePlots.add(dispIncomeHistPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(histogramIncomePlots, "Income", 2));
		    }


			if (incomeHistograms) {
				TimeSeriesSimulationPlotter EDIByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					EDIByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("EDI by Gender And Education", "£");
				}
				else {
					EDIByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("EDI by Gender And Education", "Euro");
				}
				for(Education edu: Education.values()) {
					if (Education.InEducation.equals(edu)) {
						continue;
					}
					for (Gender gender : Gender.values()) {
						GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
						Weighted_CrossSection.Double EDIWorkingCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getEquivalisedDisposableIncomeYearly", true); // Note: these are nominal values for each simulated year
						EDIWorkingCS.setFilter(genderEducationWorkingFilter);
						GenderEducationCSfilter genderEducationCSfilter = new GenderEducationCSfilter(gender, edu);
						Weighted_CrossSection.Double EDIAllCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getEquivalisedDisposableIncomeYearly", true); // Note: these are nominal values for each simulated year
						EDIAllCS.setFilter(genderEducationCSfilter);
						EDIByGenderAndEducationPlotter.addSeries("Workers (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(EDIWorkingCS), null, colorArrayList.get(colorCounter), false);
						colorCounter++;
						EDIByGenderAndEducationPlotter.addSeries("All (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(EDIAllCS), null, colorArrayList.get(colorCounter), false);
						colorCounter++;
					}
				}
				EDIByGenderAndEducationPlotter.setName("EDI by Gender / Education");
				updateChartSet.add(EDIByGenderAndEducationPlotter);
				tabSet.add(EDIByGenderAndEducationPlotter);
			}

			if (incomeHistograms) {
				TimeSeriesSimulationPlotter DispIncByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					DispIncByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Disp income by Gender And Education", "£");
				}
				else {
					DispIncByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Disp income by Gender And Education", "Euro");
				}
				for(Education edu: Education.values()) {
					if (Education.InEducation.equals(edu)) {
						continue;
					}
					for (Gender gender : Gender.values()) {
						GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
						Weighted_CrossSection.Double DispIncWorkingCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDisposableIncomeMonthlyNoNull", true); // Note: these are nominal values for each simulated year
						DispIncWorkingCS.setFilter(genderEducationWorkingFilter);
						GenderEducationCSfilter genderEducationCSfilter = new GenderEducationCSfilter(gender, edu);
						Weighted_CrossSection.Double DispIncAllCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDisposableIncomeMonthlyNoNull", true); // Note: these are nominal values for each simulated year
						DispIncAllCS.setFilter(genderEducationCSfilter);
						DispIncByGenderAndEducationPlotter.addSeries("Workers (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(DispIncWorkingCS), null, colorArrayList.get(colorCounter), false);
						colorCounter++;
						DispIncByGenderAndEducationPlotter.addSeries("All (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(DispIncAllCS), null, colorArrayList.get(colorCounter), false);
						colorCounter++;
					}
				}
				DispIncByGenderAndEducationPlotter.setName("Disp income by Gender / Education");
				updateChartSet.add(DispIncByGenderAndEducationPlotter);
				tabSet.add(DispIncByGenderAndEducationPlotter);
			}

		    // WORKING HOURS PYRAMID GRAPH
		    if (workingHoursPyramid) {
		    	Set<JInternalFrame> workingHoursPyramidPlots = new LinkedHashSet<JInternalFrame>();
			    Weighted_PyramidPlotter populationAgeGenderPlotter = new Weighted_PyramidPlotter("Working hours over time", "Total hours worked", Weighted_PyramidPlotter.DEFAULT_YAXIS, Weighted_PyramidPlotter.DEFAULT_LEFT_CAT, Weighted_PyramidPlotter.DEFAULT_RIGHT_CAT);
			    // Please note that the Pyramid plotter requires a Weighted_CrossSection[2]
			    // The exact type (int, double etc) must match the variable in Person  
			    Weighted_CrossSection.Integer[] populationData = new Weighted_CrossSection.Integer[2];
			    Weighted_CrossSection.Integer maleAgesCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "labEmpNyear", false);
			    maleAgesCS.setFilter(new GenderCSfilter(Gender.Male));
			    populationData[0] = maleAgesCS; 
			    Weighted_CrossSection.Integer femaleAgesCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "labEmpNyear", false);
			    femaleAgesCS.setFilter(new GenderCSfilter(Gender.Female)); 
			    populationData[1] = femaleAgesCS; 
			    
			    populationAgeGenderPlotter.setScalingFactor(model.getScalingFactor());
				populationAgeGenderPlotter.addCollectionSource(populationData); 
				
			    updateChartSet.add(populationAgeGenderPlotter);			//Add to set to be updated in buildSchedule method
			    workingHoursPyramidPlots.add(populationAgeGenderPlotter);
		    	
			    tabSet.add(createScrollPaneFromPlots(workingHoursPyramidPlots, "Working Hours Pyramid", 1));
		    }
		    
		    
		    //-------------------------------------------------------------------------------------------------------
		    //
	    	//	BUILD A TABBED PANE HOLDING ALL THE CHARTS THAT ONLY UPDATE AT EACH TIME-STEP (not convergence plots)
		    //
	    	//-------------------------------------------------------------------------------------------------------
		    
	    	JInternalFrame chartsFrame = new JInternalFrame("Charts");
			JTabbedPane tabbedPane = new JTabbedPane();
			chartsFrame.add(tabbedPane);
			
			for(JComponent plot: tabSet) {
				tabbedPane.addTab(plot.getName(), plot);
			}
			tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	    	chartsFrame.setResizable(true);
	    	chartsFrame.setMaximizable(true);
			GuiUtils.addWindow(chartsFrame, 300, 0, 1560, 660);
		    
			
			
		}
							
	}	



	@Override
	public void buildSchedule() {
		
		if(showCharts) {
			
			EventGroup chartingEvents = new EventGroup();
			for(JInternalFrame plot: updateChartSet) {
				chartingEvents.addEvent(plot, CommonEventType.Update);
			}
			getEngine().getEventQueue().scheduleRepeat(chartingEvents, model.getStartYear(), ordering, displayFrequency);
			getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.ResetConvergenceChart), model.getStartYear(), ordering, displayFrequency);

		}
							
	}
	
	//--------------------------------------------------------------------------
	//	Other Methods 
	//--------------------------------------------------------------------------


	//For use with bar charts to specify what colour to use based on education level
	private Color colorOfEducation(Education edu) {
		if(edu.equals(Education.Low)) {
			return Color.RED;
		}
		else if(edu.equals(Education.Medium)) {
			return Color.BLUE;
		}
		else if(edu.equals(Education.High)) {
			return Color.WHITE;
		}
		else if(edu.equals(Education.InEducation)) {
			return Color.GRAY;
		}
		else throw new IllegalArgumentException("ERROR - no color is specified for " + edu + " in SimPathsObserver class!");
	}

	
	/**
	 * Method to re-arrange JInternalFrames such as JFreeChart plots into 
	 * a single JInternalFrame (e.g. to use in a TabbedPane of plots).
	 * 
	 * @param internalFrames - a set of JInternalFrames such as JFreeChart plots 
	 * @param name - the name of the JScrollPane returned
	 * @param columns - the number of columns with which the JInternalFrames will be laid out 
	 * @return A JScrollPane laying of a set of JInternalFrames 
	 */
	private JScrollPane createScrollPaneFromPlots(Set<JInternalFrame> internalFrames, String name, int columns) {		
		
		String layoutConstraints = "wrap " + columns;
		MigLayout layout = new MigLayout(layoutConstraints, "fill, grow", "fill, grow");
		JPanel panel = new JPanel(layout);

		for(JInternalFrame internalFrame: internalFrames) {
			internalFrame.setVisible(true);
			internalFrame.setResizable(false);	//The components (charts) are not able to expand beyond their assigned row/column, so the only way to resize is to resize the whole pane. 
			panel.add(internalFrame);
		}		
		JScrollPane frame = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frame.setName(name);
		return frame;
	}
	

	//For bowker norm of labour supply/demand elasticities updated during convergence process
	public void updateConvergencePlotter() {		
		convergenceElasticitiesPlotter.update();
	}

	//For potential earnings and aggregate labour supply/demand plots updated during convergence process
	public void updateLabourMarketPlots(Region region) {

		//Potential Earnings
		for(ScatterplotSimulationPlotterRefreshable plot: potentialEarningsPlots.values()) {
			plot.update();
		}
		
		//Labour Market
		for(ScatterplotSimulationPlotterRefreshable plot: labourMarketPlots.values()) {
			plot.update();
		}
		
		//Increment iterations count
		countIterations++;
	}
		
	public void resetLabourMarketPlots(Region region) {
		
		for(ScatterplotSimulationPlotterRefreshable plot: potentialEarningsPlots.values()) {
			plot.reset();
		}

		for(ScatterplotSimulationPlotterRefreshable plot: labourMarketPlots.values()) {
			plot.reset();
		}
		countIterations = 0;

        for(var edu: Education.values()) {
            // Potential Earnings
            potentialEarningsPlots.get(edu).addSource(region.getName(),
                    () -> this.countIterations,
                    meanPotentialEarningsMultiMap.get(region, edu));
        }
    }

	
	//--------------------------------------------------------------------------
	//	Event Listener implementation 
	//--------------------------------------------------------------------------
	
	
	public enum Processes {
		ResetConvergenceChart,
	}
	
	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case ResetConvergenceChart:
			break;
			
		}
		
	}

	//--------------------------------------------------------------------------
	// Access methods
	//--------------------------------------------------------------------------
	
	public Double getDisplayFrequency() {
		return displayFrequency;
	}

	public void setDisplayFrequency(Double displayFrequency) {
		this.displayFrequency = displayFrequency;
	}
	
	public Boolean getShowCharts() {
		return showCharts;
	}

	public void setShowCharts(Boolean showCharts) {
		this.showCharts = showCharts;
	}

	public Boolean getShowAdditionalCharts() {
		return showAdditionalCharts;
	}

	public void setShowAdditionalCharts(Boolean showAdditionalCharts) {
		this.showAdditionalCharts = showAdditionalCharts;
	}

	public Boolean getShowValidationStatistics() {
		return showValidationStatistics;
	}

	public void setShowValidationStatistics(Boolean showValidationStatistics) {
		this.showValidationStatistics = showValidationStatistics;
	}

	public Integer getNumberOfHistogramBins() {
		return numberOfHistogramBins;
	}

	public void setNumberOfHistogramBins(Integer numberOfHistogramBins) {
		this.numberOfHistogramBins = numberOfHistogramBins;
	}


	public HistogramTypeEnum getHistogramType() {
		return histogramType;
	}


	public void setHistogramType(HistogramTypeEnum histogramType) {
		this.histogramType = histogramType;
	}


	public boolean isEducationByAge() {
		return educationByAge;
	}


	public void setEducationByAge(boolean educationByAge) {
		this.educationByAge = educationByAge;
	}


	public boolean isEducationByRegion() {
		return educationByRegion;
	}


	public void setEducationByRegion(boolean educationByRegion) {
		this.educationByRegion = educationByRegion;
	}


	public boolean isEducationOfAdults() {
		return educationOfAdults;
	}


	public void setEducationOfAdults(boolean educationOfAdults) {
		this.educationOfAdults = educationOfAdults;
	}


	public boolean isEmploymentByAge() {
		return employmentByAge;
	}


	public void setEmploymentByAge(boolean employmentByAge) {
		this.employmentByAge = employmentByAge;
	}


	public boolean isEmploymentByRegion() {
		return employmentByRegion;
	}


	public void setEmploymentByRegion(boolean employmentByRegion) {
		this.employmentByRegion = employmentByRegion;
	}


	public boolean isEmploymentOfAdults() {
		return employmentOfAdults;
	}


	public void setEmploymentOfAdults(boolean employmentOfAdults) {
		this.employmentOfAdults = employmentOfAdults;
	}


	public boolean isFemaleEmploymentByMaternity() {
		return femaleEmploymentByMaternity;
	}


	public void setFemaleEmploymentByMaternity(boolean femaleEmploymentByMaternity) {
		this.femaleEmploymentByMaternity = femaleEmploymentByMaternity;
	}


	public boolean isHouseholdComposition() {
		return householdComposition;
	}


	public void setHouseholdComposition(boolean householdComposition) {
		this.householdComposition = householdComposition;
	}


	public boolean isIncomeHistograms() {
		return incomeHistograms;
	}


	public void setIncomeHistograms(boolean incomeHistograms) {
		this.incomeHistograms = incomeHistograms;
	}


	public boolean isPopulationPyramid() {
		return populationPyramid;
	}


	public void setPopulationPyramid(boolean populationPyramid) {
		this.populationPyramid = populationPyramid;
	}


	public boolean isWorkingHoursPyramid() {
		return workingHoursPyramid;
	}


	public void setWorkingHoursPyramid(boolean workingHoursPyramid) {
		this.workingHoursPyramid = workingHoursPyramid;
	}


	public boolean isLabourSupply() {
		return labourSupply;
	}


	public void setLabourSupply(boolean labourSupply) {
		this.labourSupply = labourSupply;
	}


	public boolean isPopulation() {
		return population;
	}


	public void setPopulation(boolean population) {
		this.population = population;
	}


	public boolean isPoverty() {
		return poverty;
	}


	public void setPoverty(boolean poverty) {
		this.poverty = poverty;
	}


	public boolean isStudentsByAge() {
		return studentsByAge;
	}


	public void setStudentsByAge(boolean studentsByAge) {
		this.studentsByAge = studentsByAge;
	}


	public boolean isStudentsByRegion() {
		return studentsByRegion;
	}


	public void setStudentsByRegion(boolean studentsByRegion) {
		this.studentsByRegion = studentsByRegion;
	}

	public boolean isGrossEarningsByRegionAndEducation() {
		return grossEarningsByRegionAndEducation;
	}

	public void setGrossEarningsByRegionAndEducation(boolean grossEarningsByRegionAndEducation) {
		this.grossEarningsByRegionAndEducation = grossEarningsByRegionAndEducation;
	}


	public Integer getPotentialEarningsPlotMaxSamples() {
		return potentialEarningsPlotMaxSamples;
	}


	public void setPotentialEarningsPlotMaxSamples(Integer potentialEarningsPlotMaxSamples) {
		this.potentialEarningsPlotMaxSamples = potentialEarningsPlotMaxSamples;
	}


	public Integer getLabourMarketPlotMaxSamples() {
		return labourMarketPlotMaxSamples;
	}


	public void setLabourMarketPlotMaxSamples(Integer labourMarketPlotMaxSamples) {
		this.labourMarketPlotMaxSamples = labourMarketPlotMaxSamples;
	}

	public boolean isFloatingConvergencePlots() {
		return floatingConvergencePlots;
	}

	public void setFloatingConvergencePlots(boolean floatingConvergencePlots) {
		this.floatingConvergencePlots = floatingConvergencePlots;
	}


	public Integer getConvergenceElasticitiesPlotMaxSamples() {
		return convergenceElasticitiesPlotMaxSamples;
	}


	public void setConvergenceElasticitiesPlotMaxSamples(Integer convergenceElasticitiesPlotMaxSamples) {
		this.convergenceElasticitiesPlotMaxSamples = convergenceElasticitiesPlotMaxSamples;
	}

	public boolean isHealthByAge() {
		return healthByAge;
	}

	public void setHealthByAge(boolean healthByAge) {
		this.healthByAge = healthByAge;
	}

	public boolean isSecurityIndex() {
		return securityIndex;
	}

	public void setSecurityIndex(boolean securityIndex) {
		this.securityIndex = securityIndex;
	}

	public boolean isActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(boolean activityStatus) {
		this.activityStatus = activityStatus;
	}

	public boolean isHomeownershipStatus() {
		return homeownershipStatus;
	}

	public void setHomeownershipStatus(boolean homeownershipStatus) {
		this.homeownershipStatus = homeownershipStatus;
	}

}
