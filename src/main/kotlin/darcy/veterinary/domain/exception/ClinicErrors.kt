package darcy.veterinary.domain.exception

class DuplicateEntityException(message: String) : RuntimeException(message)
class EntityNotFoundException(message: String) : RuntimeException(message)
class InvalidClinicOperationException(message: String) : RuntimeException(message)
