package com.qohat.config

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.resource
import com.qohat.infra.EmailClient
import com.qohat.infra.S3Client
import com.qohat.infra.S3ClientI
import com.qohat.infra.SESClient
import com.qohat.postgres.postgres
import com.qohat.repo.ConfigRepo
import com.qohat.repo.DefaultAssignmentRepo
import com.qohat.repo.DefaultConfigRepo
import com.qohat.repo.DefaultEventRepoV2
import com.qohat.repo.DefaultListRepo
import com.qohat.repo.DefaultPaymentRepo
import com.qohat.repo.DefaultPeopleRepo
import com.qohat.repo.DefaultPeopleRequestRepo
import com.qohat.repo.DefaultProductRepo
import com.qohat.repo.DefaultRoleRepo
import com.qohat.repo.DefaultStorageRepo
import com.qohat.repo.DefaultSupplieRepo
import com.qohat.repo.DefaultUserRepo
import com.qohat.repo.DefaultViewsRepo
import com.qohat.repo.EventRepoV2
import com.qohat.repo.ListRepo
import com.qohat.repo.PaymentRepo
import com.qohat.repo.PeopleRepo
import com.qohat.repo.PeopleRequestRepo
import com.qohat.repo.ProductRepo
import com.qohat.repo.RoleRepo
import com.qohat.repo.StorageRepo
import com.qohat.repo.SupplieRepo
import com.qohat.repo.UserRepo
import com.qohat.repo.ViewsRepo
import com.qohat.services.AssignmentService
import com.qohat.services.AuthService
import com.qohat.services.DefaultAssignmentService
import com.qohat.services.DefaultAuthService
import com.qohat.services.DefaultListService
import com.qohat.services.ListService

data class Dependencies(
    val appConfig: AppConfig,
    val authService: AuthService,
    val userRepo: UserRepo,
    val peopleRepo: PeopleRepo,
    val listService: ListService,
    val assignmentService: AssignmentService,
    val roleRepo: RoleRepo,
    val peopleRequestRepo: PeopleRequestRepo,
    val s3Client: S3ClientI,
    val filesConfig: FilesConfig,
    val productRepo: ProductRepo,
    val suppliesRepo: SupplieRepo,
    val storagesRepo: StorageRepo,
    val configRepo: ConfigRepo,
    val eventRepoV2: EventRepoV2,
    val viewsRepo: ViewsRepo,
    val paymentRepo: PaymentRepo,
    val sesClient: SESClient,
    val listRepo: ListRepo
)

fun dependencies(appConfig: AppConfig): Resource<Dependencies> = resource {
    val postgres = postgres(appConfig.dbConfig).bind()
    val userRepo = DefaultUserRepo(postgres)
    val listRepo = DefaultListRepo(postgres)
    val authService = DefaultAuthService(appConfig.jwtConfig, userRepo)
    val s3Client = S3Client(appConfig.s3Config, appConfig.filesConfig)
    val peopleRepo = DefaultPeopleRepo(postgres)
    val listService = DefaultListService(listRepo)
    val paymentRepo = DefaultPaymentRepo(postgres)
    val assignmentRepo = DefaultAssignmentRepo(postgres)
    val assignmentService = DefaultAssignmentService(assignmentRepo, userRepo)
    val roleRepo = DefaultRoleRepo(postgres)
    val filesConfig = appConfig.filesConfig
    val peopleRequestRepo = DefaultPeopleRequestRepo(postgres)
    val productRepo = DefaultProductRepo(postgres)
    val supplieRepo = DefaultSupplieRepo(postgres)
    val storageRepo = DefaultStorageRepo(postgres)
    val configRepo = DefaultConfigRepo(postgres)
    val eventRepoV2 = DefaultEventRepoV2(postgres)
    val viewsRepo = DefaultViewsRepo(postgres)
    val sesClient = EmailClient(appConfig.smtpConfig)
    Dependencies(
        appConfig, authService, userRepo, peopleRepo, listService, assignmentService, roleRepo, peopleRequestRepo,
        s3Client, filesConfig, productRepo, supplieRepo, storageRepo, configRepo, eventRepoV2, viewsRepo, paymentRepo,
        sesClient, listRepo
    )
}
