package gov.usgs.aqcu.reference;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import gov.usgs.aqcu.config.AquariusReferenceListConfig;


@Repository
public class ComputationReferenceService {
	private static final Logger LOG = LoggerFactory.getLogger(ComputationReferenceService.class);
	private AquariusReferenceListConfig aquariusReferenceListConfig;

	@Autowired
	public ComputationReferenceService(AquariusReferenceListConfig aquariusReferenceListConfig) {
		this.aquariusReferenceListConfig = aquariusReferenceListConfig;
	}

	public List<String> get() {
		//TODO: Cache this list somewhere else
		return aquariusReferenceListConfig.getComputations();
	}
}