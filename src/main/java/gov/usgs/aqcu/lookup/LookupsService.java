package gov.usgs.aqcu.lookup;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.LocationDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.UnitMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import gov.usgs.aqcu.model.LocationBasicData;
import gov.usgs.aqcu.model.TimeSeriesBasicData;
import gov.usgs.aqcu.parameter.FieldVisitDatesRequestParameters;
import gov.usgs.aqcu.parameter.FindInDerivationChainRequestParameters;
import gov.usgs.aqcu.parameter.GetUpchainRatingModelsRequestParameters;
import gov.usgs.aqcu.parameter.ProcessorTypesRequestParameters;
import gov.usgs.aqcu.parameter.SiteSearchRequestParameters;
import gov.usgs.aqcu.parameter.TimeSeriesIdentifiersRequestParameters;
import gov.usgs.aqcu.reference.ComputationReferenceService;
import gov.usgs.aqcu.reference.ControlConditionReferenceService;
import gov.usgs.aqcu.reference.PeriodReferenceService;
import gov.usgs.aqcu.retrieval.DerivationChainSearchService;
import gov.usgs.aqcu.retrieval.FieldVisitDescriptionListService;
import gov.usgs.aqcu.retrieval.LocationDescriptionListService;
import gov.usgs.aqcu.retrieval.ProcessorTypesService;
import gov.usgs.aqcu.retrieval.TimeSeriesDescriptionListService;
import gov.usgs.aqcu.retrieval.UnitsLookupService;
import gov.usgs.aqcu.retrieval.UpchainRatingModelSearchService;
import gov.usgs.aqcu.util.TimeSeriesUtils;

@Repository
public class LookupsService {
	private static final Logger LOG = LoggerFactory.getLogger(LookupsService.class);
	private TimeSeriesDescriptionListService timeSeriesDescriptionListService;
	private ProcessorTypesService processorTypesService;
	private LocationDescriptionListService locationDescriptionListService;
	private ComputationReferenceService computationReferenceService;
	private ControlConditionReferenceService controlConditionReferenceService;
	private PeriodReferenceService periodReferenceService;
	private UnitsLookupService unitsLookupService;
	private FieldVisitDescriptionListService fieldVisitDescriptionListService;
	private DerivationChainSearchService derivationChainService;
	private UpchainRatingModelSearchService upchainRatingModelSearchService;

	@Autowired
	public LookupsService(
		TimeSeriesDescriptionListService timeSeriesDescriptionListService,
		ProcessorTypesService processorTypesService,
		LocationDescriptionListService locationDescriptionListService,
		UnitsLookupService unitsLookupService,
		ComputationReferenceService computationReferenceService,
		ControlConditionReferenceService controlConditionReferenceService,
		PeriodReferenceService periodReferenceService,
		FieldVisitDescriptionListService fieldVisitDescriptionListService,
		DerivationChainSearchService derivationChainService,
		UpchainRatingModelSearchService upchainRatingModelSearchService) {
			this.timeSeriesDescriptionListService = timeSeriesDescriptionListService;
			this.processorTypesService = processorTypesService;
			this.locationDescriptionListService = locationDescriptionListService;
			this.unitsLookupService = unitsLookupService;
			this.computationReferenceService = computationReferenceService;
			this.controlConditionReferenceService = controlConditionReferenceService;
			this.periodReferenceService = periodReferenceService;
			this.fieldVisitDescriptionListService = fieldVisitDescriptionListService;
			this.derivationChainService = derivationChainService;
			this.upchainRatingModelSearchService = upchainRatingModelSearchService;
	}

	public Map<String,TimeSeriesBasicData> getTimeSeriesDescriptions(TimeSeriesIdentifiersRequestParameters params) {
		List<TimeSeriesDescription> descs = timeSeriesDescriptionListService.getTimeSeriesDescriptionList(params.getComputationIdentifier(), params.getComputationPeriodIdentifier(),
			params.getStationId(), params.getParameter(), params.getPublish(), params.getPrimary());
		return descs.stream().map(d -> new TimeSeriesBasicData(d)).collect(Collectors.toMap(TimeSeriesBasicData::getUniqueId, Function.identity()));
	}

	public List<String> searchDerivationChain(FindInDerivationChainRequestParameters params) {
		ZoneOffset primaryZoneOffset = getZoneOffset(params.getTimeSeriesIdentifier());
		List<TimeSeriesDescription> descList = derivationChainService.findTimeSeriesInDerivationChain(params.getTimeSeriesIdentifier(), params.getDirection(), params.getPrimary(), null, params.getParameter(), 
			params.getComputationIdentifier(), params.getComputationPeriodIdentifier(), params.getStartInstant(primaryZoneOffset), params.getEndInstant(primaryZoneOffset), params.getFullChain());
		return descList.stream().map(d -> d.getUniqueId()).collect(Collectors.toList());
	}

	public List<String> getRatingModel(GetUpchainRatingModelsRequestParameters params) {
		ZoneOffset primaryZoneOffset = getZoneOffset(params.getTimeSeriesIdentifier());
		return upchainRatingModelSearchService.getRatingModelsUpchain(params.getTimeSeriesIdentifier(), params.getStartInstant(primaryZoneOffset), params.getEndInstant(primaryZoneOffset), params.getFullChain());
	}

	public Map<String, List<String>> getProcessorTypes(ProcessorTypesRequestParameters params) {
		ZoneOffset primaryZoneOffset = getZoneOffset(params.getTimeSeriesIdentifier());
		return processorTypesService.getProcessorTypes(params.getTimeSeriesIdentifier(), params.getStartInstant(primaryZoneOffset), params.getEndInstant(primaryZoneOffset));
	}

	public List<LocationBasicData> searchSites(SiteSearchRequestParameters params) {
		List<LocationDescription> siteDescList = locationDescriptionListService.searchSites(params.getSiteNumber(), params.getPageSize());
		return siteDescList.stream().map(d -> new LocationBasicData(d)).collect(Collectors.toList());
	}

	public List<String> getFieldVisitDates(FieldVisitDatesRequestParameters params) {
		return fieldVisitDescriptionListService.getFieldVisitDates(params.getSiteNumber());
	}

	public List<Map<String,String>> getControlConditions() {
		return controlConditionReferenceService.get();
	}

	public List<String> getComputations() {
		return computationReferenceService.get();
	}

	public List<String> getPeriods() {
		 return periodReferenceService.get();
	}

	public List<String> getUnits() {
		List<UnitMetadata> unitMetadataList = unitsLookupService.getUnits();
		return unitMetadataList.stream().map(u -> u.getIdentifier()).collect(Collectors.toList());
	}

	protected ZoneOffset getZoneOffset(String timeSeriesIdentifier) {
		TimeSeriesDescription primaryDescription = null;

		if(timeSeriesIdentifier != null) {
			primaryDescription = timeSeriesDescriptionListService.getTimeSeriesDescription(timeSeriesIdentifier);
		}
		
		return TimeSeriesUtils.getZoneOffset(primaryDescription);
	}
}