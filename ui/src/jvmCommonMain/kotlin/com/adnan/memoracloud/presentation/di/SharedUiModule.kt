package com.adnan.memoracloud.presentation.di

import com.adnan.memoracloud.presentation.AppViewModel
import com.adnan.memoracloud.presentation.channels.ChannelsViewModel
import com.adnan.memoracloud.presentation.collection.CollectionViewModel
import com.adnan.memoracloud.presentation.files.FileClipboard
import com.adnan.memoracloud.presentation.files.FilesViewModel
import com.adnan.memoracloud.presentation.gallery.GalleryViewModel
import com.adnan.memoracloud.presentation.home.HomeViewModel
import com.adnan.memoracloud.presentation.onboarding.OnboardingViewModel
import com.adnan.memoracloud.presentation.note.NoteEditorViewModel
import com.adnan.memoracloud.presentation.preview.PreviewContentResolver
import com.adnan.memoracloud.presentation.preview.PreviewViewModel
import com.adnan.memoracloud.presentation.proxy.ProxyViewModel
import com.adnan.memoracloud.presentation.search.SearchViewModel
import com.adnan.memoracloud.presentation.settings.ExclusionsViewModel
import com.adnan.memoracloud.presentation.settings.SettingsViewModel
import com.adnan.memoracloud.presentation.transfers.TransfersViewModel
import com.adnan.memoracloud.presentation.trash.TrashViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedUiModule = module {
    singleOf(::PreviewContentResolver)
    singleOf(::FileClipboard)
    viewModelOf(::AppViewModel)
    viewModelOf(::PreviewViewModel)
    viewModelOf(::ChannelsViewModel)
    viewModelOf(::CollectionViewModel)
    viewModelOf(::FilesViewModel)
    viewModelOf(::GalleryViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::NoteEditorViewModel)
    viewModelOf(::ProxyViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ExclusionsViewModel)
    viewModelOf(::TransfersViewModel)
    viewModelOf(::TrashViewModel)
}
