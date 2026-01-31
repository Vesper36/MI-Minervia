package edu.minervia.platform.service.async

import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.service.identity.IdentityGenerationService
import edu.minervia.platform.service.identity.llm.LlmPolishService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Separate component for registration step execution.
 * Extracted to ensure @Transactional works correctly (avoiding self-invocation).
 * Per CONSTRAINT [AI-STEP-TRANSACTION]: Each step runs in independent transaction.
 */
@Component
class RegistrationStepExecutor(
    private val identityGenerationService: IdentityGenerationService,
    private val llmPolishService: LlmPolishService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Step 1: Generate identity using rules engine.
     * Transaction committed after this step completes.
     */
    @Transactional
    fun executeIdentityRulesStep(application: RegistrationApplication) {
        log.debug("Executing identity rules step for application: {}", application.id)
        // Identity generation is handled by IdentityGenerationService
        // This step validates the application data and prepares for generation
        // TODO: Integrate with IdentityGenerationService when student creation is triggered
    }

    /**
     * Step 2: Polish identity with LLM.
     * Transaction committed after this step completes.
     */
    @Transactional
    fun executeLlmPolishStep(application: RegistrationApplication) {
        log.debug("Executing LLM polish step for application: {}", application.id)
        // LLM polish is handled by LlmPolishService with timeout
        // TODO: Integrate with LlmPolishService when identity polish is triggered
    }

    /**
     * Step 3: Generate photos using FLUX.1.
     * Transaction committed after this step completes.
     */
    @Transactional
    fun executePhotoGenerationStep(application: RegistrationApplication) {
        log.debug("Executing photo generation step for application: {}", application.id)
        // Photo generation would call FLUX.1 API
        // TODO: Integrate with photo generation service when implemented
    }
}
